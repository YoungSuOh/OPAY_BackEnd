package com.opay.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Review Request DTO
 * 리뷰 작성/수정 요청 시 사용되는 DTO
 */
@Getter
@Setter
public class ReviewRequest {

    /**
     * 상품 ID
     * 리뷰를 작성할 상품의 고유 ID
     */
    @NotNull(message = "상품 ID는 필수입니다")
    private Long productId;

    /**
     * 평점 (1-5)
     * 1점부터 5점까지의 정수 값만 허용
     */
    @NotNull(message = "평점은 필수입니다")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다")
    private Integer rating;

    /**
     * 리뷰 내용
     * 최대 2000자까지 입력 가능
     */
    @Size(max = 2000, message = "리뷰 내용은 2000자 이하여야 합니다")
    private String content;
}
