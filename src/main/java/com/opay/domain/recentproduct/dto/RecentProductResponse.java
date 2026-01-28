package com.opay.domain.recentproduct.dto;

import com.opay.domain.recentproduct.entity.RecentProduct;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * RecentProduct Response DTO
 * 최근 본 상품 조회 응답 시 사용되는 DTO
 */
@Getter
@Builder
public class RecentProductResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Long productPrice;
    private Double averageRating;
    private Integer reviewCount;
    private LocalDateTime viewedAt;

    /**
     * RecentProduct Entity를 RecentProductResponse로 변환
     * 엔티티의 정보를 클라이언트에 전달하기 위한 응답 DTO로 변환
     */
    public static RecentProductResponse from(RecentProduct recentProduct) {
        return RecentProductResponse.builder()
                .id(recentProduct.getId())
                .productId(recentProduct.getProduct().getId())
                .productName(recentProduct.getProduct().getName())
                .productImageUrl(recentProduct.getProduct().getImageUrl())
                .productPrice(recentProduct.getProduct().getPrice())
                .averageRating(recentProduct.getProduct().getAverageRating())
                .reviewCount(recentProduct.getProduct().getReviewCount())
                .viewedAt(recentProduct.getViewedAt())
                .build();
    }
}
