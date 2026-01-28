package com.opay.domain.order.dto;

import com.opay.domain.orderitem.entity.OrderItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * OrderItem Response DTO
 * 주문 상품 조회 응답 시 사용되는 DTO
 */
@Getter
@Builder
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer quantity;
    private Long price; // 주문 시점 가격 (스냅샷)
    private Long totalPrice; // price * quantity
    private LocalDateTime createdAt;

    /**
     * OrderItem Entity를 OrderItemResponse로 변환
     * 엔티티의 정보를 클라이언트에 전달하기 위한 응답 DTO로 변환
     */
    public static OrderItemResponse from(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProduct().getId())
                .productName(orderItem.getProduct().getName())
                .productImageUrl(orderItem.getProduct().getImageUrl())
                .quantity(orderItem.getQuantity())
                .price(orderItem.getPrice())
                .totalPrice(orderItem.getTotalPrice())
                .createdAt(orderItem.getCreatedAt())
                .build();
    }
}
