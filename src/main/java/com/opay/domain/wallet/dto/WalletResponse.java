package com.opay.domain.wallet.dto;

import com.opay.domain.wallet.entity.Wallet;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Wallet Response DTO
 * 지갑 응답 시 사용되는 DTO
 */
@Getter
@Builder
public class WalletResponse {

    private Long walletId;
    private Long userId;
    private Long balance;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Wallet 엔티티로부터 DTO 생성
     */
    public static WalletResponse from(Wallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getId())
                .userId(wallet.getUser().getId())
                .balance(wallet.getBalance())
                .version(wallet.getVersion())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
