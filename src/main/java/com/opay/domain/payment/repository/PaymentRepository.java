package com.opay.domain.payment.repository;

import com.opay.domain.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Payment Repository
 * 결제 데이터 접근을 위한 Repository
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 멱등성 키로 결제 조회
     * 
     * @param idempotencyKey 멱등성 키
     * @return 결제 Optional
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * 주문 ID로 결제 목록 조회
     * 
     * @param orderId 주문 ID
     * @return 결제 목록
     */
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * 사용자 ID로 결제 목록 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return 결제 페이지
     */
    @Query("SELECT p FROM Payment p WHERE p.order.user.id = :userId ORDER BY p.createdAt DESC")
    Page<Payment> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 주문 ID와 상태로 결제 조회
     * 
     * @param orderId 주문 ID
     * @param status 결제 상태
     * @return 결제 목록
     */
    List<Payment> findByOrderIdAndStatus(Long orderId, Payment.PaymentStatus status);

    /**
     * 전체 결제 목록 조회 (관리자용, 최신순)
     */
    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(Payment.PaymentStatus status);

    /**
     * 관리자: 결제 검색 (주문 ID, 회원 ID, 회원명/이메일 키워드)
     */
    @Query("SELECT p FROM Payment p WHERE " +
           "(:orderId IS NULL OR p.order.id = :orderId) AND " +
           "(:userId IS NULL OR p.order.user.id = :userId) AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(p.order.user.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.order.user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<Payment> findForAdmin(@Param("orderId") Long orderId, @Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);
}
