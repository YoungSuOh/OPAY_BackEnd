package com.opay.domain.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Wallet Charge Request DTO
 * 지갑 충전 요청 시 사용되는 DTO
 */
@Getter
@Setter
public class WalletChargeRequest {

    /**
     * 충전할 금액
     */
    @NotNull(message = "충전 금액은 필수입니다")
    @Positive(message = "충전 금액은 0보다 커야 합니다")
    private Long amount;
}
