package com.opay.domain.recentproduct.repository;

import com.opay.domain.recentproduct.entity.RecentProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * RecentProduct Repository
 * 최근 본 상품 데이터 접근을 위한 JPA Repository
 */
public interface RecentProductRepository extends JpaRepository<RecentProduct, Long> {
    
    /**
     * 특정 사용자의 최근 본 상품 목록 조회 (최신순)
     * 최근 조회한 상품부터 표시하기 위해 조회 시각 기준 내림차순 정렬
     */
    Page<RecentProduct> findByUserIdOrderByViewedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 특정 사용자와 상품의 최근 본 상품 조회
     * 동일 상품 재조회 시 기존 레코드를 찾아 조회 시각을 업데이트할 때 사용
     */
    Optional<RecentProduct> findByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * 특정 사용자의 최근 본 상품 개수 조회
     * 최대 개수 제한을 확인할 때 사용
     */
    long countByUserId(Long userId);
    
    /**
     * 오래된 최근 본 상품 삭제
     * 최대 개수를 초과할 때 가장 오래된 항목을 삭제할 때 사용
     */
    @Modifying
    @Query("DELETE FROM RecentProduct rp WHERE rp.user.id = :userId " +
           "AND rp.viewedAt = (SELECT MIN(rp2.viewedAt) FROM RecentProduct rp2 WHERE rp2.user.id = :userId)")
    void deleteOldestByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 시각 이전의 최근 본 상품 삭제
     * 일정 기간이 지난 최근 본 상품을 정리할 때 사용
     */
    @Modifying
    @Query("DELETE FROM RecentProduct rp WHERE rp.viewedAt < :beforeDate")
    void deleteByViewedAtBefore(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * 특정 사용자의 최근 본 상품 전체 삭제
     * 사용자가 최근 본 상품 목록을 초기화할 때 사용
     */
    @Modifying
    @Query("DELETE FROM RecentProduct rp WHERE rp.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
