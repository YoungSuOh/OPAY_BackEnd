package com.opay.domain.order.repository;

import com.opay.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Repository
 * 주문 데이터 접근을 위한 JPA Repository
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * 특정 사용자의 주문 목록 조회 (최신순)
     * 사용자의 주문 내역을 조회할 때 사용
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 특정 사용자의 주문 목록 조회 (상태별)
     * 특정 상태의 주문만 필터링하여 조회할 때 사용
     */
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Order.OrderStatus status, Pageable pageable);
    
    /**
     * 특정 기간의 주문 목록 조회
     * 통계나 리포트를 위해 특정 기간의 주문을 조회할 때 사용
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    /**
     * 특정 사용자의 주문 개수 조회
     * 사용자의 총 주문 수를 조회할 때 사용
     */
    long countByUserId(Long userId);
    
    /**
     * 특정 사용자의 특정 기간 주문 개수 조회
     * 이달의 주문 건수 등을 조회할 때 사용
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId " +
           "AND o.createdAt BETWEEN :startDate AND :endDate")
    long countByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * 관리자: 주문 검색 (주문 ID, 회원 ID, 회원명/이메일 키워드)
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(:orderId IS NULL OR o.id = :orderId) AND " +
           "(:userId IS NULL OR o.user.id = :userId) AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(o.user.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(o.user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY o.createdAt DESC")
    Page<Order> findForAdmin(@Param("orderId") Long orderId, @Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);
}
