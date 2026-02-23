package com.opay.domain.transaction.repository;

import com.opay.domain.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Transaction Repository
 * 거래 데이터 접근을 위한 Repository
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * 멱등성 키로 거래 조회
     * 
     * @param idempotencyKey 멱등성 키
     * @return 거래 Optional
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * 사용자 ID로 거래 목록 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return 거래 페이지
     */
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 지갑 ID로 거래 목록 조회
     * 
     * @param walletId 지갑 ID
     * @param pageable 페이지 정보
     * @return 거래 페이지
     */
    Page<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    /**
     * 주문 ID로 거래 목록 조회
     * 
     * @param orderId 주문 ID
     * @return 거래 목록
     */
    List<Transaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * 결제 ID로 거래 목록 조회
     * 
     * @param paymentId 결제 ID
     * @return 거래 목록
     */
    List<Transaction> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    /**
     * PENDING 상태인 거래 목록 조회 (재처리 대상)
     * 
     * @param beforeTime 이 시간 이전의 PENDING 거래만 조회
     * @return 거래 목록
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.createdAt < :beforeTime")
    List<Transaction> findPendingTransactionsBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 기간별 거래 목록 조회
     * 
     * @param userId 사용자 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 거래 목록
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.createdAt >= :startDate AND t.createdAt <= :endDate " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdAndDateRange(@Param("userId") Long userId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * 정산 대상 거래 목록 조회 (SUCCESS 상태만)
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 거래 목록
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'SUCCESS' " +
           "AND t.createdAt >= :startDate AND t.createdAt <= :endDate " +
           "ORDER BY t.createdAt ASC")
    List<Transaction> findSettlementTransactions(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);
}
