package com.opay.domain.cart.repository;

import com.opay.domain.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Cart Repository
 * 장바구니 데이터 접근을 위한 JPA Repository
 */
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    /**
     * 특정 사용자의 모든 장바구니 항목 조회
     * 장바구니 페이지에서 사용자의 모든 장바구니 항목을 표시할 때 사용
     */
    List<Cart> findByUserIdOrderByAddedAtDesc(Long userId);
    
    /**
     * 특정 사용자와 상품의 장바구니 항목 조회
     * 동일 상품 재추가 시 기존 항목을 찾아 수량을 증가시킬 때 사용
     */
    Optional<Cart> findByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * 특정 사용자의 장바구니 항목 개수 조회
     * 장바구니 아이콘에 표시할 항목 수를 조회할 때 사용
     */
    long countByUserId(Long userId);
    
    /**
     * 특정 사용자의 장바구니 전체 삭제
     * 주문 완료 후 장바구니를 비울 때 사용
     */
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 사용자의 장바구니 항목 존재 여부 확인
     * 장바구니가 비어있는지 확인할 때 사용
     */
    boolean existsByUserId(Long userId);
    
    /**
     * 특정 상품이 장바구니에 담겨있는지 확인
     * 상품 삭제 시 장바구니에 담겨있는지 확인할 때 사용
     */
    boolean existsByProductId(Long productId);
}
