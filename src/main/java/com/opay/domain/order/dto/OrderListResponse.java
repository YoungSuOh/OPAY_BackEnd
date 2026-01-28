package com.opay.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Order List Response DTO
 * 주문 목록 조회 응답 시 사용되는 DTO (페이지네이션 포함)
 */
@Getter
@Builder
public class OrderListResponse {
    private List<OrderResponse> orders;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}
