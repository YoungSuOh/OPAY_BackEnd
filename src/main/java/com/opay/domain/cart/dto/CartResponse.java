package com.opay.domain.cart.dto;

import com.opay.domain.cart.entity.Cart;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Cart Response DTO
 * 장바구니 조회 응답 시 사용되는 DTO
 */
@Getter
@Builder
public class CartResponse {
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Long productPrice;
    private Integer stock;
    private Integer quantity;
    private Long totalPrice; // 상품 가격 * 수량
    private LocalDateTime addedAt;

    /**
     * Cart Entity를 CartResponse로 변환
     * 엔티티의 정보를 클라이언트에 전달하기 위한 응답 DTO로 변환
     * 총 가격은 상품 가격 * 수량으로 계산
     */
    public static CartResponse from(Cart cart) {
        Long totalPrice = cart.getProduct().getPrice() * cart.getQuantity();
        
        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .productId(cart.getProduct().getId())
                .productName(cart.getProduct().getName())
                .productImageUrl(cart.getProduct().getImageUrl())
                .productPrice(cart.getProduct().getPrice())
                .stock(cart.getProduct().getStock())
                .quantity(cart.getQuantity())
                .totalPrice(totalPrice)
                .addedAt(cart.getAddedAt())
                .build();
    }
}
