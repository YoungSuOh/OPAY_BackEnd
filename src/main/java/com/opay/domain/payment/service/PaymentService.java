package com.opay.domain.payment.service;

import com.opay.domain.order.entity.Order;
import com.opay.domain.order.repository.OrderRepository;
import com.opay.domain.payment.entity.Payment;
import com.opay.domain.payment.repository.PaymentRepository;
import com.opay.domain.transaction.entity.Transaction;
import com.opay.domain.transaction.service.TransactionService;
import com.opay.domain.wallet.entity.Wallet;
import com.opay.domain.wallet.repository.WalletRepository;
import com.opay.domain.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Payment Service
 * 결제 관련 비즈니스 로직을 처리하는 서비스
 * - 멱등성 보장
 * - 결제 처리 흐름 관리
 * - 장애 시 재처리 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;

    /**
     * 결제 요청 (Payment 생성)
     * 멱등성 보장: 동일한 idempotency_key로 이미 생성된 결제가 있으면 반환
     * 
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @param method 결제 수단
     * @param idempotencyKey 멱등성 키
     * @return 결제 (기존 또는 신규)
     */
    @Transactional
    public Payment requestPayment(Long orderId, Long amount, 
                                 Payment.PaymentMethod method, String idempotencyKey) {
        // 멱등성 체크: 동일한 idempotency_key로 이미 생성된 결제가 있는지 확인
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("중복 결제 요청 무시: idempotencyKey={}, paymentId={}", 
                    idempotencyKey, existing.get().getId());
            return existing.get();
        }

        // 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        // 결제 금액 검증
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다");
        }
        if (amount > order.getTotalAmount() - order.getPaidAmount()) {
            throw new IllegalArgumentException("결제 금액이 주문 잔액을 초과합니다");
        }

        // Payment 생성 (READY 상태)
        Payment payment = Payment.builder()
                .order(order)
                .amount(amount)
                .method(method)
                .status(Payment.PaymentStatus.READY)
                .idempotencyKey(idempotencyKey)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("결제 요청 생성: paymentId={}, orderId={}, amount={}, method={}", 
                saved.getId(), orderId, amount, method);
        
        return saved;
    }

    /**
     * 결제 승인 (결제 처리)
     * 1. Payment 생성 (멱등)
     * 2. Transaction(PENDING) 생성
     * 3. Wallet 잔액 차감(원자적)
     * 4. 성공 → Transaction SUCCESS
     * 5. Payment SUCCESS
     * 
     * @param paymentId 결제 ID
     * @param idempotencyKey 멱등성 키 (검증용)
     * @return 결제
     */
    @Transactional
    public Payment approvePayment(Long paymentId, String idempotencyKey) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다: " + paymentId));

        // 멱등성 키 검증
        if (!payment.getIdempotencyKey().equals(idempotencyKey)) {
            throw new IllegalArgumentException("멱등성 키가 일치하지 않습니다");
        }

        // 결제 상태 확인
        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            log.info("이미 처리된 결제: paymentId={}", paymentId);
            return payment;
        }

        if (!payment.canRetry()) {
            throw new IllegalStateException("재시도할 수 없는 결제 상태입니다: " + payment.getStatus());
        }

        // 결제 시작
        payment.startPayment();

        try {
            Long userId = payment.getOrder().getUser().getId();
            Long orderId = payment.getOrder().getId();

            // 1. 지갑 조회
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다: " + userId));

            // 2. Transaction(PENDING) 생성
            Transaction transaction = transactionService.createTransaction(
                    userId,
                    wallet.getId(),
                    payment.getId(),
                    orderId,
                    Transaction.TransactionType.PAY,
                    payment.getAmount(),
                    idempotencyKey + "_transaction"
            );

            // 3. Wallet 잔액 차감(원자적)
            boolean deducted = walletService.deductBalance(userId, payment.getAmount());
            
            if (!deducted) {
                // 잔액 부족
                transaction.markAsFail();
                payment.markAsFail();
                log.error("결제 실패 (잔액 부족): paymentId={}, userId={}, amount={}", 
                        paymentId, userId, payment.getAmount());
                throw new IllegalArgumentException("잔액이 부족합니다");
            }

            // 4. 성공 → Transaction SUCCESS
            transactionService.markTransactionAsSuccess(transaction.getId());

            // 5. Payment SUCCESS
            payment.markAsSuccess();

            // 주문 결제 금액 업데이트
            payment.getOrder().updatePaidAmount(
                    payment.getOrder().getPaidAmount() + payment.getAmount());

            log.info("결제 승인 완료: paymentId={}, transactionId={}, userId={}, amount={}", 
                    paymentId, transaction.getId(), userId, payment.getAmount());

            return payment;

        } catch (Exception e) {
            // 장애 발생 시 실패 처리
            payment.markAsFail();
            log.error("결제 처리 중 오류 발생: paymentId={}, error={}", paymentId, e.getMessage());
            throw new RuntimeException("결제 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 결제 상태 조회
     * 
     * @param paymentId 결제 ID
     * @return 결제
     */
    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다: " + paymentId));
    }

    /**
     * 멱등성 키로 결제 조회
     * 
     * @param idempotencyKey 멱등성 키
     * @return 결제 Optional
     */
    public Optional<Payment> getPaymentByIdempotencyKey(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey);
    }

    /**
     * 주문 결제 목록 조회
     * 
     * @param orderId 주문 ID
     * @return 결제 목록
     */
    public List<Payment> getPaymentsByOrder(Long orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    /**
     * 사용자 결제 목록 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return 결제 페이지
     */
    public Page<Payment> getPaymentsByUser(Long userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable);
    }

    /**
     * 결제 취소
     * 
     * @param paymentId 결제 ID
     */
    @Transactional
    public void cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다: " + paymentId));

        payment.cancel();
        log.info("결제 취소: paymentId={}", paymentId);
    }

    /**
     * PENDING 상태인 Transaction 재처리
     * 장애 발생 시 미완료된 거래를 재처리
     * 
     * @param transactionId 거래 ID
     * @return 재처리 성공 여부
     */
    @Transactional
    public boolean retryPendingTransaction(Long transactionId) {
        // Transaction은 TransactionService에서 직접 조회해야 함
        // 여기서는 Payment와 연결된 Transaction만 재처리
        // 실제 구현은 배치 작업에서 처리하는 것이 좋음
        log.info("PENDING 거래 재처리 요청: transactionId={}", transactionId);
        return false;
    }
}
