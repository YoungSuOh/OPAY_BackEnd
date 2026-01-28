package com.opay.domain.cart.entity;

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
 * Cart Entity
 * 사용자의 장바구니 정보를 저장하는 엔티티
 * - 사용자와 상품의 조합으로 장바구니 항목 관리
 * - 동일 상품 재추가 시 수량 증가
 */
@Entity
@Table(name = "carts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "product_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 장바구니 소유자 (사용자)
     * Many-to-One 관계: 한 사용자는 여러 장바구니 항목을 가질 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 장바구니에 담긴 상품
     * Many-to-One 관계: 한 상품은 여러 사용자의 장바구니에 담길 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * 상품 수량
     * 기본값은 1이며, 동일 상품 재추가 시 증가
     */
    @Column(nullable = false)
    private Integer quantity = 1;

    /**
     * 장바구니에 담은 시각
     * 자동으로 생성 시각이 저장됨
     */
    @CreatedDate
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @Builder
    public Cart(User user, Product product, Integer quantity) {
        this.user = user;
        this.product = product;
        this.quantity = quantity != null ? quantity : 1;
    }

    /**
     * 수량 증가
     * 동일 상품을 다시 장바구니에 추가할 때 수량을 증가시킴
     */
    public void increaseQuantity(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다");
        }
        this.quantity += amount;
    }

    /**
     * 수량 업데이트
     * 장바구니에서 수량을 직접 변경할 때 사용
     */
    public void updateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다");
        }
        this.quantity = quantity;
    }
}
