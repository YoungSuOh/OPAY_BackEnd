package com.opay.domain.refund.entity;

import com.opay.domain.order.entity.Order;
import com.opay.domain.payment.entity.Payment;
import com.opay.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(nullable = false)
    private Long amount;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RefundStatus status = RefundStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id")
    private User processedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum RefundStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    @Builder
    public RefundRequest(Order order, Payment payment, Long amount, String reason, User requestedBy) {
        this.order = order;
        this.payment = payment;
        this.amount = amount;
        this.reason = reason;
        this.requestedBy = requestedBy;
    }

    public void approve(User processedBy) {
        if (this.status != RefundStatus.PENDING) {
            throw new IllegalStateException("대기 중인 환불만 처리할 수 있습니다");
        }
        this.status = RefundStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = processedBy;
    }

    public void reject(User processedBy) {
        if (this.status != RefundStatus.PENDING) {
            throw new IllegalStateException("대기 중인 환불만 처리할 수 있습니다");
        }
        this.status = RefundStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = processedBy;
    }
}
