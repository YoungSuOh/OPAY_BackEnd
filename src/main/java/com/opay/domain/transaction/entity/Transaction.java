package com.opay.domain.transaction.entity;

import com.opay.domain.order.entity.Order;
import com.opay.domain.user.entity.User;
import com.opay.domain.wallet.entity.Wallet;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Transaction Entity
 * 모든 돈의 이동을 기록하는 엔티티 (append-only)
 * - 결제 시스템의 심장
 * - 복구 기준, 정산 기준
 * - 과거 기록을 덮어쓰지 않음
 */
@Entity
@Table(name = "transactions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_transaction_idempotency_key", columnNames = "idempotency_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 지갑
     * Many-to-One 관계: 한 지갑은 여러 거래를 가짐
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /**
     * 사용자
     * Many-to-One 관계: 한 사용자는 여러 거래를 가짐
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 결제 ID (nullable)
     * 결제와 연결된 거래인 경우
     */
    @Column(name = "payment_id")
    private Long paymentId;

    /**
     * 주문 ID (nullable)
     * 주문과 연결된 거래인 경우
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    /**
     * 거래 유형
     * PAY: 결제
     * REFUND: 환불
     * EARN: 적립
     * CHARGE: 충전
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    /**
     * 거래 금액
     * 항상 양수 값 (증가/감소는 type으로 구분)
     */
    @Column(nullable = false)
    private Long amount;

    /**
     * 거래 상태
     * PENDING: 미완료 (재처리 대상)
     * SUCCESS: 성공 (정산 대상)
     * FAIL: 실패
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING;

    /**
     * 멱등성 키
     * 중복 거래 방지를 위한 고유 키
     * UNIQUE 제약으로 중복 INSERT 차단
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /**
     * 거래 생성 시각
     * append-only이므로 created_at만 존재
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Transaction(Wallet wallet, User user, Long paymentId, Order order,
                       TransactionType type, Long amount, TransactionStatus status,
                       String idempotencyKey) {
        this.wallet = wallet;
        this.user = user;
        this.paymentId = paymentId;
        this.order = order;
        this.type = type;
        this.amount = amount;
        this.status = status != null ? status : TransactionStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * 거래 상태를 SUCCESS로 변경
     * PENDING 상태에서만 가능
     */
    public void markAsSuccess() {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태인 거래만 SUCCESS로 변경할 수 있습니다");
        }
        this.status = TransactionStatus.SUCCESS;
    }

    /**
     * 거래 상태를 FAIL로 변경
     * PENDING 상태에서만 가능
     */
    public void markAsFail() {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태인 거래만 FAIL로 변경할 수 있습니다");
        }
        this.status = TransactionStatus.FAIL;
    }

    /**
     * 거래 유형 열거형
     */
    public enum TransactionType {
        PAY,        // 결제 (차감)
        REFUND,     // 환불 (증가)
        EARN,       // 적립 (증가)
        CHARGE      // 충전 (증가)
    }

    /**
     * 거래 상태 열거형
     */
    public enum TransactionStatus {
        PENDING,   // 미완료 (재처리 대상)
        SUCCESS,   // 성공 (정산 대상)
        FAIL       // 실패
    }
}
