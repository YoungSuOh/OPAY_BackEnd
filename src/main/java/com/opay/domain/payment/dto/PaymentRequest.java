package com.opay.domain.payment.dto;

import com.opay.domain.payment.entity.Payment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Payment Request DTO
 * 결제 요청 시 사용되는 DTO
 */
@Getter
@Setter
public class PaymentRequest {

    /**
     * 주문 ID
     */
    @NotNull(message = "주문 ID는 필수입니다")
    private Long orderId;

    /**
     * 결제 금액
     */
    @NotNull(message = "결제 금액은 필수입니다")
    @Positive(message = "결제 금액은 0보다 커야 합니다")
    private Long amount;

    /**
     * 결제 수단
     */
    @NotNull(message = "결제 수단은 필수입니다")
    private Payment.PaymentMethod method;

    /**
     * 멱등성 키
     * 중복 결제 방지를 위한 고유 키
     */
    @NotBlank(message = "멱등성 키는 필수입니다")
    private String idempotencyKey;
}
