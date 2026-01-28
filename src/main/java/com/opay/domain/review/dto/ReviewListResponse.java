package com.opay.domain.review.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Review List Response DTO
 * 리뷰 목록 조회 응답 시 사용되는 DTO (페이지네이션 포함)
 */
@Getter
@Builder
public class ReviewListResponse {
    private List<ReviewResponse> reviews;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}
