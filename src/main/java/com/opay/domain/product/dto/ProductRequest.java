package com.opay.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 100, message = "상품명은 100자 이하여야 합니다")
    private String name;

    @Size(max = 2000, message = "상품 설명은 2000자 이하여야 합니다")
    private String description;

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private Long price;

    @Min(value = 0, message = "재고는 0 이상이어야 합니다")
    private Integer stock = 0;

    @Size(max = 50, message = "카테고리는 50자 이하여야 합니다")
    private String category;

    @Size(max = 255, message = "이미지 URL은 255자 이하여야 합니다")
    private String imageUrl;
}
