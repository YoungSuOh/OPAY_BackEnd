package com.opay.domain.review.entity;

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
 * Review Entity
 * 상품 리뷰 정보를 저장하는 엔티티
 * - 사용자와 상품에 대한 리뷰 작성
 * - 평점(1-5)과 리뷰 내용 저장
 */
@Entity
@Table(name = "reviews", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "product_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 리뷰를 작성한 사용자
     * Many-to-One 관계: 한 사용자는 여러 리뷰 작성 가능
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 리뷰 대상 상품
     * Many-to-One 관계: 한 상품은 여러 리뷰를 가질 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * 평점 (1-5)
     * 필수 값이며, 1점부터 5점까지의 정수 값
     */
    @Column(nullable = false)
    private Integer rating;

    /**
     * 리뷰 내용
     * 텍스트 형태로 저장되며, 선택적 필드
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 리뷰 작성 시각
     * 자동으로 생성 시각이 저장됨
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Review(User user, Product product, Integer rating, String content) {
        this.user = user;
        this.product = product;
        this.rating = rating;
        this.content = content;
    }

    /**
     * 리뷰 내용 수정
     * 평점과 내용을 수정할 수 있음
     */
    public void update(Integer rating, String content) {
        if (rating != null) {
            this.rating = rating;
        }
        if (content != null) {
            this.content = content;
        }
    }
}
