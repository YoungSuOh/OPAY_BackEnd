package com.opay.domain.orderitem.repository;

import com.opay.domain.orderitem.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * OrderItem Repository
 * 주문 상품 데이터 접근을 위한 JPA Repository
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    /**
     * 특정 주문의 모든 주문 상품 조회
     * 주문 상세 정보를 조회할 때 주문에 포함된 모든 상품을 조회
     */
    List<OrderItem> findByOrderIdOrderByCreatedAtAsc(Long orderId);
    
    /**
     * 특정 사용자의 모든 주문 상품 조회
     * 사용자의 모든 주문 상품을 조회할 때 사용
     */
    List<OrderItem> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 특정 주문의 주문 상품 개수 조회
     * 주문에 포함된 상품 개수를 조회할 때 사용
     */
    long countByOrderId(Long orderId);
    
    /**
     * 특정 상품이 포함된 주문 상품 조회
     * 상품별 판매 통계를 위해 사용
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId")
    List<OrderItem> findByProductId(@Param("productId") Long productId);
}
