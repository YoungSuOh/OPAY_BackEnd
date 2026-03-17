package com.opay.admin.dto;

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
public class AdminRefundResponse {
    private Long id;
    private Long orderId;
    private Long paymentId;
    private Long amount;
    private String reason;
    private RefundRequest.RefundStatus status;
    private Long requestedByUserId;
    private String requestedByEmail;
    private LocalDateTime processedAt;
    private Long processedByUserId;
    private LocalDateTime createdAt;

    public static AdminRefundResponse from(RefundRequest r) {
        return AdminRefundResponse.builder()
                .id(r.getId())
                .orderId(r.getOrder().getId())
                .paymentId(r.getPayment() != null ? r.getPayment().getId() : null)
                .amount(r.getAmount())
                .reason(r.getReason())
                .status(r.getStatus())
                .requestedByUserId(r.getRequestedBy().getId())
                .requestedByEmail(r.getRequestedBy().getEmail())
                .processedAt(r.getProcessedAt())
                .processedByUserId(r.getProcessedBy() != null ? r.getProcessedBy().getId() : null)
                .createdAt(r.getCreatedAt())
                .build();
    }
}
