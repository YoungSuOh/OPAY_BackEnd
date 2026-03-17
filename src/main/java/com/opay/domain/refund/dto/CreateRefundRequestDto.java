package com.opay.domain.refund.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateRefundRequestDto {
    @NotNull(message = "주문 ID는 필수입니다")
    private Long orderId;
    private Long amount;  // null이면 주문 결제 금액 전체
    private String reason;
}
