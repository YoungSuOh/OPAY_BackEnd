package com.opay.domain.recentproduct.entity;

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
 * RecentProduct Entity
 * 사용자가 최근 조회한 상품 정보를 저장하는 엔티티
 * - 사용자가 상품 상세 페이지를 조회할 때 기록
 * - 동일 상품 재조회 시 조회 시각만 업데이트
 * - 최대 N개 유지 (서비스 로직에서 처리)
 */
@Entity
@Table(name = "recent_products", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "product_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RecentProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 최근 본 상품을 조회한 사용자
     * Many-to-One 관계: 한 사용자는 여러 최근 본 상품을 가질 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 조회한 상품
     * Many-to-One 관계: 한 상품은 여러 사용자에 의해 조회될 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * 상품 조회 시각
     * 동일 상품 재조회 시 이 값이 업데이트됨
     */
    @CreatedDate
    @Column(name = "viewed_at", nullable = false, updatable = true)
    private LocalDateTime viewedAt;

    @Builder
    public RecentProduct(User user, Product product) {
        this.user = user;
        this.product = product;
    }

    /**
     * 조회 시각 업데이트
     * 동일 상품을 다시 조회할 때 조회 시각을 현재 시각으로 업데이트
     */
    public void updateViewedAt() {
        this.viewedAt = LocalDateTime.now();
    }
}
