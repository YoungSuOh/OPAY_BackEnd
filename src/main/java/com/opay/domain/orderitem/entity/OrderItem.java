package com.opay.domain.orderitem.entity;

import com.opay.domain.order.entity.Order;
import com.opay.domain.product.entity.Product;
import com.opay.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * OrderItem Entity
 * 주문에 포함된 상품 상세 정보를 저장하는 엔티티
 * - 주문 시점의 상품 가격을 스냅샷으로 저장 (가격 변경 대비)
 * - 주문 수량과 주문 시점 가격을 저장
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주문한 사용자
     * Many-to-One 관계: 한 사용자는 여러 주문 상품을 가질 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 주문
     * Many-to-One 관계: 한 주문은 여러 주문 상품을 포함할 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    /**
     * 주문 설정
     * 주문 생성 후 OrderItem에 주문 정보를 설정할 때 사용
     */
    public void setOrder(Order order) {
        this.order = order;
    }

    /**
     * 주문한 상품
     * Many-to-One 관계: 한 상품은 여러 주문에 포함될 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * 주문 수량
     * 주문한 상품의 개수
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * 주문 시점 가격 (스냅샷)
     * 주문 시점의 상품 가격을 저장하여 가격 변경에 영향받지 않음
     */
    @Column(nullable = false)
    private Long price;

    /**
     * 주문 상품 생성 시각
     * 자동으로 생성 시각이 저장됨
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public OrderItem(User user, Order order, Product product, Integer quantity, Long price) {
        this.user = user;
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    /**
     * 주문 상품 총 가격 계산
     * 주문 수량 * 주문 시점 가격
     */
    public Long getTotalPrice() {
        return this.price * this.quantity;
    }
}
