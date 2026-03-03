package com.opay.domain.search.controller;

import com.opay.domain.product.dto.ProductListResponse;
import com.opay.domain.search.service.PopularKeywordService;
import com.opay.domain.search.service.ProductSearchService;
import com.opay.domain.search.service.SearchAutocompleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService productSearchService;
    private final SearchAutocompleteService searchAutocompleteService;
    private final PopularKeywordService popularKeywordService;

    @GetMapping
    public ResponseEntity<ProductListResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false, defaultValue = "recent") String sort,
            @RequestParam(required = false, defaultValue = "desc") String order,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        if (q != null && !q.trim().isEmpty()) {
            popularKeywordService.recordSearch(q.trim());
        }

        ProductListResponse response = productSearchService.search(
                q, category, minPrice, maxPrice, sort, order, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@RequestParam String q) {
        List<String> suggestions = searchAutocompleteService.getSuggestions(q);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<String>> popularKeywords() {
        List<String> keywords = popularKeywordService.getPopularKeywords();
        return ResponseEntity.ok(keywords);
    }
}
