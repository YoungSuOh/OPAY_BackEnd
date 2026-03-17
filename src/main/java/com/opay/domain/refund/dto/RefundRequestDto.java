package com.opay.domain.refund.dto;

import com.opay.domain.refund.entity.RefundRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDto {
    private Long id;
    private Long orderId;
    private Long amount;
    private String reason;
    private RefundRequest.RefundStatus status;
    private LocalDateTime createdAt;

    public static RefundRequestDto from(RefundRequest r) {
        return RefundRequestDto.builder()
                .id(r.getId())
                .orderId(r.getOrder().getId())
                .amount(r.getAmount())
                .reason(r.getReason())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
