package com.opay.domain.transaction.dto;

import com.opay.domain.transaction.entity.Transaction;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Transaction Response DTO
 * 거래 응답 시 사용되는 DTO
 */
@Getter
@Builder
public class TransactionResponse {

    private Long transactionId;
    private Long walletId;
    private Long userId;
    private Long paymentId;
    private Long orderId;
    private Transaction.TransactionType type;
    private Long amount;
    private Transaction.TransactionStatus status;
    private String idempotencyKey;
    private LocalDateTime createdAt;

    /**
     * Transaction 엔티티로부터 DTO 생성
     */
    public static TransactionResponse from(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .walletId(transaction.getWallet().getId())
                .userId(transaction.getUser().getId())
                .paymentId(transaction.getPaymentId())
                .orderId(transaction.getOrder() != null ? transaction.getOrder().getId() : null)
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .idempotencyKey(transaction.getIdempotencyKey())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
