package com.opay.domain.wallet.service;

import com.opay.domain.user.repository.UserRepository;
import com.opay.domain.wallet.entity.Wallet;
import com.opay.domain.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지갑 존재 보장 전용 컴포넌트
 * REQUIRES_NEW 트랜잭션으로 실행해, 호출자 트랜잭션의 영속 컨텍스트에 Wallet이 남지 않도록 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEnsureHolder {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureWalletExists(Long userId) {
        if (walletRepository.findByUserId(userId).isPresent()) {
            return;
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(0L)
                .build();
        Wallet saved = walletRepository.save(wallet);
        log.info("지갑 생성: userId={}, walletId={}", userId, saved.getId());
    }
}
