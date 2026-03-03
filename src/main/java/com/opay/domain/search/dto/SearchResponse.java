package com.opay.domain.search.dto;

import com.opay.domain.product.dto.ProductListResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SearchResponse {
    private ProductListResponse results;
    private List<String> popularKeywords;
    private List<String> relatedKeywords;
}
