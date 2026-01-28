package com.opay.domain.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Cart Request DTO
 * 장바구니 추가/수정 요청 시 사용되는 DTO
 */
@Getter
@Setter
public class CartRequest {

    /**
     * 상품 ID
     * 장바구니에 추가할 상품의 고유 ID
     */
    @NotNull(message = "상품 ID는 필수입니다")
    private Long productId;

    /**
     * 수량
     * 기본값은 1이며, 최소 1 이상이어야 함
     */
    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다")
    private Integer quantity = 1;
}
