package com.opay.domain.order.entity;

import com.opay.domain.user.entity.User;
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
 * Order Entity
 * 주문 정보를 저장하는 엔티티
 * - 사용자의 주문 정보를 관리
 * - 주문 상태를 추적 (PENDING, CONFIRMED, PREPARING, SHIPPED, DELIVERED, CANCELLED)
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주문한 사용자
     * Many-to-One 관계: 한 사용자는 여러 주문을 할 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 주문 총 금액
     * 주문에 포함된 모든 상품의 가격 합계
     */
    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    /**
     * 결제 완료 금액
     * 실제로 결제가 완료된 금액 (부분 결제 가능)
     */
    @Column(name = "paid_amount", nullable = false)
    private Long paidAmount = 0L;

    /**
     * 주문 상태
     * PENDING: 주문 대기
     * CONFIRMED: 주문 확정
     * PREPARING: 준비 중
     * SHIPPED: 배송 중
     * DELIVERED: 배송 완료
     * CANCELLED: 취소됨
     */
    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * 주문 생성 시각
     * 자동으로 생성 시각이 저장됨
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 주문 수정 시각
     * 주문 정보가 수정될 때마다 업데이트됨
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Order(User user, Long totalAmount, Long paidAmount, OrderStatus status) {
        this.user = user;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount != null ? paidAmount : 0L;
        this.status = status != null ? status : OrderStatus.PENDING;
    }

    /**
     * 주문 상태 업데이트
     * 주문 상태를 변경할 때 사용
     */
    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    /**
     * 결제 금액 업데이트
     * 결제가 완료되면 결제 완료 금액을 업데이트
     */
    public void updatePaidAmount(Long paidAmount) {
        if (paidAmount < 0) {
            throw new IllegalArgumentException("결제 금액은 0 이상이어야 합니다");
        }
        if (paidAmount > this.totalAmount) {
            throw new IllegalArgumentException("결제 금액은 주문 총 금액을 초과할 수 없습니다");
        }
        this.paidAmount = paidAmount;
    }

    /**
     * 주문 취소
     * 주문을 취소할 때 상태를 CANCELLED로 변경
     */
    public void cancel() {
        if (this.status == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("배송 완료된 주문은 취소할 수 없습니다");
        }
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * 주문 상태 열거형
     */
    public enum OrderStatus {
        PENDING,      // 주문 대기
        CONFIRMED,    // 주문 확정
        PREPARING,    // 준비 중
        SHIPPED,      // 배송 중
        DELIVERED,    // 배송 완료
        CANCELLED     // 취소됨
    }
}
