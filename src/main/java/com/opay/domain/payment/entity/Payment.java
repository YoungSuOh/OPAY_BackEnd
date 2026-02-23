package com.opay.domain.payment.entity;

import com.opay.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Payment Entity
 * 결제 시도 단위를 나타내는 엔티티
 * - 사용자의 의도 표현
 * - 실패/재시도 관리
 * - 비즈니스 상태 관리
 */
@Entity
@Table(name = "payments", uniqueConstraints = {
    @UniqueConstraint(name = "uk_payment_idempotency_key", columnNames = "idempotency_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주문 ID
     * Many-to-One 관계: 한 주문은 여러 결제 시도를 가질 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * 결제 금액
     */
    @Column(nullable = false)
    private Long amount;

    /**
     * 결제 수단
     * CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, TOSS, KAKAO, NAVER 등
     */
    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    /**
     * 결제 상태
     * READY: 준비 (재시도 가능)
     * PAYING: 결제 중
     * SUCCESS: 성공 (불변)
     * FAIL: 실패 (재시도 가능)
     * CANCELED: 취소됨
     */
    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.READY;

    /**
     * 멱등성 키
     * 중복 결제 방지를 위한 고유 키
     * UNIQUE 제약으로 중복 INSERT 차단
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /**
     * 결제 승인 시각
     * SUCCESS 상태일 때만 값이 있음
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Payment(Order order, Long amount, PaymentMethod method, 
                   PaymentStatus status, String idempotencyKey) {
        this.order = order;
        this.amount = amount;
        this.method = method;
        this.status = status != null ? status : PaymentStatus.READY;
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * 결제 시작 (PAYING 상태로 변경)
     * READY 또는 FAIL 상태에서만 가능
     */
    public void startPayment() {
        if (this.status != PaymentStatus.READY && this.status != PaymentStatus.FAIL) {
            throw new IllegalStateException("READY 또는 FAIL 상태인 결제만 시작할 수 있습니다");
        }
        this.status = PaymentStatus.PAYING;
    }

    /**
     * 결제 성공 처리
     * PAYING 상태에서만 가능
     */
    public void markAsSuccess() {
        if (this.status != PaymentStatus.PAYING) {
            throw new IllegalStateException("PAYING 상태인 결제만 SUCCESS로 변경할 수 있습니다");
        }
        this.status = PaymentStatus.SUCCESS;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 결제 실패 처리
     * PAYING 상태에서만 가능
     */
    public void markAsFail() {
        if (this.status != PaymentStatus.PAYING) {
            throw new IllegalStateException("PAYING 상태인 결제만 FAIL로 변경할 수 있습니다");
        }
        this.status = PaymentStatus.FAIL;
    }

    /**
     * 결제 취소
     * SUCCESS 상태가 아닌 경우에만 가능
     */
    public void cancel() {
        if (this.status == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("SUCCESS 상태인 결제는 취소할 수 없습니다");
        }
        this.status = PaymentStatus.CANCELED;
    }

    /**
     * 재시도 가능 여부 확인
     * READY 또는 FAIL 상태일 때만 재시도 가능
     */
    public boolean canRetry() {
        return this.status == PaymentStatus.READY || this.status == PaymentStatus.FAIL;
    }

    /**
     * 결제 수단 열거형
     */
    public enum PaymentMethod {
        CARD,              // 카드 결제
        BANK_TRANSFER,     // 계좌이체
        VIRTUAL_ACCOUNT,   // 가상계좌
        TOSS,              // 토스페이
        KAKAO,             // 카카오페이
        NAVER              // 네이버페이
    }

    /**
     * 결제 상태 열거형
     */
    public enum PaymentStatus {
        READY,     // 준비 (재시도 가능)
        PAYING,    // 결제 중
        SUCCESS,   // 성공 (불변)
        FAIL,      // 실패 (재시도 가능)
        CANCELED   // 취소됨
    }
}
