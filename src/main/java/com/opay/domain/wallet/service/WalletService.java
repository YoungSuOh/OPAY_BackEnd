package com.opay.domain.wallet.service;

import com.opay.domain.user.entity.User;
import com.opay.domain.user.repository.UserRepository;
import com.opay.domain.wallet.entity.Wallet;
import com.opay.domain.wallet.repository.WalletRepository;
import com.opay.domain.wallet.repository.WalletVersionBalance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wallet Service
 * 지갑 관련 비즈니스 로직을 처리하는 서비스
 * - 동시성 제어: 버전만 조회 후 벌크 UPDATE (낙관적 동시성, 커밋 시 버전 충돌 방지)
 * - 음수 잔액 방지: 차감 시 WHERE balance >= amount
 * - 원자적 업데이트: 단일 UPDATE 문
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletEnsureHolder walletEnsureHolder;

    /**
     * 사용자 지갑 조회
     * 지갑이 없으면 생성
     *
     * @param userId 사용자 ID
     * @return 지갑
     */
    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

                    Wallet wallet = Wallet.builder()
                            .user(user)
                            .balance(0L)
                            .build();

                    Wallet saved = walletRepository.save(wallet);
                    log.info("지갑 생성: userId={}, walletId={}", userId, saved.getId());
                    return saved;
                });
    }

    /**
     * 잔액 조회
     * 
     * @param userId 사용자 ID
     * @return 잔액
     */
    public Long getBalance(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return wallet.getBalance();
    }

    /**
     * 잔액 차감 (원자적 연산)
     * 버전만 조회 후 벌크 UPDATE로 동시성·음수 잔액 방지
     *
     * @param userId 사용자 ID
     * @param amount 차감할 금액
     * @return 차감 성공 여부
     */
    @Transactional
    public boolean deductBalance(Long userId, Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다");
        }

        walletEnsureHolder.ensureWalletExists(userId);

        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            Integer version = walletRepository.findVersionByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("지갑이 존재하지 않습니다: userId=" + userId));

            int updated = walletRepository.deductBalance(userId, amount, version);

            if (updated > 0) {
                log.info("잔액 차감 성공: userId={}, amount={}", userId, amount);
                return true;
            }

            // updated == 0: 버전 충돌 또는 잔액 부족
            var versionBalance = walletRepository.findVersionAndBalanceByUserId(userId).orElse(null);
            if (versionBalance != null && versionBalance.getBalance() < amount) {
                log.warn("잔액 부족: userId={}, balance={}, amount={}",
                        userId, versionBalance.getBalance(), amount);
                return false;
            }

            log.warn("잔액 차감 재시도 (버전 충돌): userId={}, retryCount={}", userId, retryCount);
            retryCount++;
        }

        log.error("잔액 차감 실패 (최대 재시도 초과): userId={}, amount={}", userId, amount);
        return false;
    }

    /**
     * 잔액 증가 (원자적 연산)
     * 버전만 조회 후 벌크 UPDATE로 동시성 제어 (커밋 시 버전 충돌 없음)
     *
     * @param userId 사용자 ID
     * @param amount 증가할 금액
     */
    @Transactional
    public void addBalance(Long userId, Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("증가 금액은 0보다 커야 합니다");
        }

        walletEnsureHolder.ensureWalletExists(userId);

        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            Integer version = walletRepository.findVersionByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("지갑이 존재하지 않습니다: userId=" + userId));

            int updated = walletRepository.addBalance(userId, amount, version);

            if (updated > 0) {
                log.info("잔액 증가 성공: userId={}, amount={}", userId, amount);
                return;
            }

            log.warn("잔액 증가 재시도 (버전 충돌): userId={}, retryCount={}", userId, retryCount);
            retryCount++;
        }

        throw new RuntimeException("잔액 증가 실패 (최대 재시도 초과): userId=" + userId);
    }

    /**
     * 잔액 충전
     * 
     * @param userId 사용자 ID
     * @param amount 충전할 금액
     */
    @Transactional
    public void chargeBalance(Long userId, Long amount) {
        addBalance(userId, amount);
    }
}
