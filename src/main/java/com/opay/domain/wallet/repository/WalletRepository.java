package com.opay.domain.wallet.repository;

import com.opay.domain.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Wallet Repository
 * 지갑 데이터 접근을 위한 Repository
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * 사용자 ID로 지갑 조회
     * 
     * @param userId 사용자 ID
     * @return 지갑 Optional
     */
    Optional<Wallet> findByUserId(Long userId);

    /**
     * 사용자 ID로 지갑 조회 (낙관적 잠금)
     * 동시성 제어를 위해 사용 (벌크 UPDATE와 함께 사용 시 커밋 시 버전 충돌 발생하므로, 잔액 증감에는 사용하지 않음)
     *
     * @param userId 사용자 ID
     * @return 지갑 Optional
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Optional<Wallet> findByUserIdWithLock(@Param("userId") Long userId);

    /**
     * 사용자 ID로 지갑 버전만 조회
     * 벌크 UPDATE 전 버전 확인용 (엔티티를 영속 컨텍스트에 올리지 않음)
     *
     * @param userId 사용자 ID
     * @return 버전 Optional
     */
    @Query("SELECT w.version FROM Wallet w WHERE w.user.id = :userId")
    Optional<Integer> findVersionByUserId(@Param("userId") Long userId);

    /**
     * 사용자 ID로 지갑 버전·잔액만 조회
     * 차감 실패 시 잔액 부족 vs 버전 충돌 구분용
     *
     * @param userId 사용자 ID
     * @return 버전·잔액 프로젝션 Optional
     */
    @Query("SELECT w.version AS version, w.balance AS balance FROM Wallet w WHERE w.user.id = :userId")
    Optional<WalletVersionBalance> findVersionAndBalanceByUserId(@Param("userId") Long userId);

    /**
     * 잔액 차감 (원자적 연산)
     * 조건부 업데이트로 동시성 제어
     * 
     * @param userId 사용자 ID
     * @param amount 차감할 금액
     * @param version 현재 버전
     * @return 업데이트된 행 수 (1이면 성공, 0이면 실패)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Wallet w SET w.balance = w.balance - :amount, w.version = w.version + 1 " +
           "WHERE w.user.id = :userId AND w.balance >= :amount AND w.version = :version")
    int deductBalance(@Param("userId") Long userId, 
                      @Param("amount") Long amount, 
                      @Param("version") Integer version);

    /**
     * 잔액 증가 (원자적 연산)
     * 
     * @param userId 사용자 ID
     * @param amount 증가할 금액
     * @param version 현재 버전
     * @return 업데이트된 행 수
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount, w.version = w.version + 1 " +
           "WHERE w.user.id = :userId AND w.version = :version")
    int addBalance(@Param("userId") Long userId, 
                   @Param("amount") Long amount, 
                   @Param("version") Integer version);
}
