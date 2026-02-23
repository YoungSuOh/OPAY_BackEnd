package com.opay.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Payment Approve Request DTO
 * 결제 승인 요청 시 사용되는 DTO
 */
@Getter
@Setter
public class PaymentApproveRequest {

    /**
     * 결제 ID
     */
    @NotNull(message = "결제 ID는 필수입니다")
    private Long paymentId;

    /**
     * 멱등성 키 (검증용)
     */
    @NotBlank(message = "멱등성 키는 필수입니다")
    private String idempotencyKey;
}
