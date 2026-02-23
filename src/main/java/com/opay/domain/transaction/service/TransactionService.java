package com.opay.domain.transaction.service;

import com.opay.domain.order.entity.Order;
import com.opay.domain.order.repository.OrderRepository;
import com.opay.domain.transaction.entity.Transaction;
import com.opay.domain.transaction.repository.TransactionRepository;
import com.opay.domain.user.entity.User;
import com.opay.domain.user.repository.UserRepository;
import com.opay.domain.wallet.entity.Wallet;
import com.opay.domain.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Transaction Service
 * 거래 관련 비즈니스 로직을 처리하는 서비스
 * - PENDING 상태 관리
 * - 재처리 로직
 * - append-only 원장 구조
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    /**
     * 거래 생성 (멱등성 보장)
     * idempotency_key로 중복 생성 방지
     * 
     * @param userId 사용자 ID
     * @param walletId 지갑 ID
     * @param paymentId 결제 ID (nullable)
     * @param orderId 주문 ID (nullable)
     * @param type 거래 유형
     * @param amount 거래 금액
     * @param idempotencyKey 멱등성 키
     * @return 거래 (기존 또는 신규)
     */
    @Transactional
    public Transaction createTransaction(Long userId, Long walletId, Long paymentId, 
                                        Long orderId, Transaction.TransactionType type, 
                                        Long amount, String idempotencyKey) {
        // 멱등성 체크: 동일한 idempotency_key로 이미 생성된 거래가 있는지 확인
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("중복 거래 요청 무시: idempotencyKey={}, transactionId={}", 
                    idempotencyKey, existing.get().getId());
            return existing.get();
        }

        // 엔티티 조회
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("지갑을 찾을 수 없습니다: " + walletId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Order order = null;
        if (orderId != null) {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
        }

        // 거래 생성 (PENDING 상태)
        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .user(user)
                .paymentId(paymentId)
                .order(order)
                .type(type)
                .amount(amount)
                .status(Transaction.TransactionStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("거래 생성: transactionId={}, userId={}, type={}, amount={}, status=PENDING", 
                saved.getId(), userId, type, amount);
        
        return saved;
    }

    /**
     * 거래 조회 (단일)
     * 
     * @param transactionId 거래 ID
     * @return 거래
     */
    public Transaction getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다: " + transactionId));
    }

    /**
     * 거래 성공 처리
     * PENDING 상태의 거래를 SUCCESS로 변경
     * 
     * @param transactionId 거래 ID
     */
    @Transactional
    public void markTransactionAsSuccess(Long transactionId) {
        Transaction transaction = getTransaction(transactionId);
        transaction.markAsSuccess();
        log.info("거래 성공 처리: transactionId={}", transactionId);
    }

    /**
     * 거래 실패 처리
     * PENDING 상태의 거래를 FAIL로 변경
     * 
     * @param transactionId 거래 ID
     */
    @Transactional
    public void markTransactionAsFail(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다: " + transactionId));

        transaction.markAsFail();
        log.info("거래 실패 처리: transactionId={}", transactionId);
    }

    /**
     * 멱등성 키로 거래 조회
     * 
     * @param idempotencyKey 멱등성 키
     * @return 거래 Optional
     */
    public Optional<Transaction> getTransactionByIdempotencyKey(String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey);
    }

    /**
     * 사용자 거래 목록 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return 거래 페이지
     */
    public Page<Transaction> getTransactionsByUser(Long userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 지갑 거래 목록 조회
     * 
     * @param walletId 지갑 ID
     * @param pageable 페이지 정보
     * @return 거래 페이지
     */
    public Page<Transaction> getTransactionsByWallet(Long walletId, Pageable pageable) {
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable);
    }

    /**
     * 주문 거래 목록 조회
     * 
     * @param orderId 주문 ID
     * @return 거래 목록
     */
    public List<Transaction> getTransactionsByOrder(Long orderId) {
        return transactionRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    /**
     * 결제 거래 목록 조회
     * 
     * @param paymentId 결제 ID
     * @return 거래 목록
     */
    public List<Transaction> getTransactionsByPayment(Long paymentId) {
        return transactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }

    /**
     * PENDING 상태인 거래 목록 조회 (재처리 대상)
     * 
     * @param beforeMinutes 이 시간(분) 이전의 PENDING 거래만 조회
     * @return 거래 목록
     */
    public List<Transaction> getPendingTransactions(int beforeMinutes) {
        LocalDateTime beforeTime = LocalDateTime.now().minusMinutes(beforeMinutes);
        return transactionRepository.findPendingTransactionsBefore(beforeTime);
    }

    /**
     * 기간별 거래 목록 조회
     * 
     * @param userId 사용자 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 거래 목록
     */
    public List<Transaction> getTransactionsByDateRange(Long userId, 
                                                       LocalDateTime startDate, 
                                                       LocalDateTime endDate) {
        return transactionRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * 정산 대상 거래 목록 조회 (SUCCESS 상태만)
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 거래 목록
     */
    public List<Transaction> getSettlementTransactions(LocalDateTime startDate, 
                                                        LocalDateTime endDate) {
        return transactionRepository.findSettlementTransactions(startDate, endDate);
    }
}
