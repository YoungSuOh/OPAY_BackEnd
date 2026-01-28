package com.opay.domain.review.repository;

import com.opay.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Review Repository
 * 리뷰 데이터 접근을 위한 JPA Repository
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    /**
     * 특정 상품의 리뷰 목록 조회 (페이지네이션)
     * 상품 상세 페이지에서 리뷰 목록을 표시할 때 사용
     */
    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);
    
    /**
     * 특정 사용자의 리뷰 목록 조회 (페이지네이션)
     * 마이페이지에서 내가 작성한 리뷰를 조회할 때 사용
     */
    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 특정 사용자가 특정 상품에 작성한 리뷰 조회
     * 중복 리뷰 작성 방지 및 리뷰 수정 시 사용
     */
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * 특정 상품의 리뷰 개수 조회
     * 상품의 리뷰 수를 빠르게 조회할 때 사용
     */
    long countByProductId(Long productId);
    
    /**
     * 특정 상품의 평균 평점 계산
     * 상품의 평균 평점을 계산하여 업데이트할 때 사용
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double calculateAverageRatingByProductId(@Param("productId") Long productId);
    
    /**
     * 특정 상품의 리뷰 존재 여부 확인
     * 상품 삭제 시 리뷰가 있는지 확인할 때 사용
     */
    boolean existsByProductId(Long productId);
    
    /**
     * 특정 사용자의 리뷰 존재 여부 확인
     * 사용자 삭제 시 리뷰가 있는지 확인할 때 사용
     */
    boolean existsByUserId(Long userId);
}
