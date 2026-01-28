package com.opay.domain.order.dto;

import com.opay.domain.order.entity.Order;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Response DTO
 * 주문 조회 응답 시 사용되는 DTO
 */
@Getter
@Builder
public class OrderResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long totalAmount;
    private Long paidAmount;
    private Order.OrderStatus status;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Order Entity를 OrderResponse로 변환
     * 엔티티의 정보를 클라이언트에 전달하기 위한 응답 DTO로 변환
     */
    public static OrderResponse from(Order order, List<OrderItemResponse> items) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName())
                .totalAmount(order.getTotalAmount())
                .paidAmount(order.getPaidAmount())
                .status(order.getStatus())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
