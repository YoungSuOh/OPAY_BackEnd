package com.opay.domain.cart.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Cart List Response DTO
 * 장바구니 목록 조회 응답 시 사용되는 DTO
 */
@Getter
@Builder
public class CartListResponse {
    private List<CartResponse> items;
    private long totalItems; // 전체 항목 수
    private long totalAmount; // 전체 금액 (모든 항목의 총 가격 합계)
}
