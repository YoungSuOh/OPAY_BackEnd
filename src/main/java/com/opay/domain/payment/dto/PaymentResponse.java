package com.opay.domain.payment.dto;

import com.opay.domain.payment.entity.Payment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Payment Response DTO
 * 결제 응답 시 사용되는 DTO
 */
@Getter
@Builder
public class PaymentResponse {

    private Long paymentId;
    private Long orderId;
    private Long amount;
    private Payment.PaymentMethod method;
    private Payment.PaymentStatus status;
    private String idempotencyKey;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Payment 엔티티로부터 DTO 생성
     */
    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .idempotencyKey(payment.getIdempotencyKey())
                .approvedAt(payment.getApprovedAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
