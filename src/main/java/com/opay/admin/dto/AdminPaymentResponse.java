package com.opay.admin.dto;

import com.opay.domain.payment.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentResponse {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private String userEmail;
    private Long amount;
    private Payment.PaymentMethod method;
    private Payment.PaymentStatus status;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;

    public static AdminPaymentResponse from(Payment payment) {
        return AdminPaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .userId(payment.getOrder().getUser().getId())
                .userEmail(payment.getOrder().getUser().getEmail())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .approvedAt(payment.getApprovedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
