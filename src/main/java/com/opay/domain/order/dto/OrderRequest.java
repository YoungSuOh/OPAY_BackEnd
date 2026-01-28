package com.opay.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Order Request DTO
 * 주문 생성 요청 시 사용되는 DTO
 */
@Getter
@Setter
public class OrderRequest {

    /**
     * 주문할 상품 목록
     * 장바구니에서 선택한 상품들 또는 직접 선택한 상품들
     */
    @NotEmpty(message = "주문할 상품 목록은 필수입니다")
    @Valid
    private List<OrderItemRequest> items;

    /**
     * OrderItem Request DTO
     * 주문에 포함될 상품 정보
     */
    @Getter
    @Setter
    public static class OrderItemRequest {
        /**
         * 상품 ID
         */
        @NotNull(message = "상품 ID는 필수입니다")
        private Long productId;

        /**
         * 주문 수량
         */
        @NotNull(message = "수량은 필수입니다")
        private Integer quantity;
    }
}
