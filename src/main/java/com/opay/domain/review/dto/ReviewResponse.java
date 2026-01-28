package com.opay.domain.review.dto;

import com.opay.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Review Response DTO
 * 리뷰 조회 응답 시 사용되는 DTO
 */
@Getter
@Builder
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long productId;
    private String productName;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;

    /**
     * Review Entity를 ReviewResponse로 변환
     * 엔티티의 정보를 클라이언트에 전달하기 위한 응답 DTO로 변환
     */
    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
