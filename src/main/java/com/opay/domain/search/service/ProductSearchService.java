package com.opay.domain.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.opay.domain.product.dto.ProductListResponse;
import com.opay.domain.product.dto.ProductResponse;
import com.opay.domain.product.entity.Product;
import com.opay.domain.product.repository.ProductRepository;
import com.opay.domain.product.service.ProductService;
import com.opay.domain.search.document.ProductDocument;
import com.opay.domain.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductSearchRepository productSearchRepository;
    private final ProductService productService;
    private final ProductRepository productRepository;

    private static final int SEARCH_TIMEOUT_MS = 150;

    public ProductListResponse search(
            String keyword,
            String category,
            Long minPrice,
            Long maxPrice,
            String sortBy,
            String sortDirection,
            int page,
            int size) {

        try {
            return searchWithElasticsearch(keyword, category, minPrice, maxPrice, sortBy, sortDirection, page, size);
        } catch (Exception e) {
            log.warn("Elasticsearch 검색 실패, MySQL fallback: {}", e.getMessage());
            return productService.getProducts(keyword, category, minPrice, maxPrice, mapSortBy(sortBy), sortDirection, page, size);
        }
    }

    private ProductListResponse searchWithElasticsearch(
            String keyword,
            String category,
            Long minPrice,
            Long maxPrice,
            String sortBy,
            String sortDirection,
            int page,
            int size) {

        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            Query multiMatch = MultiMatchQuery.of(m -> m
                    .query(keyword.trim())
                    .fields("name^2", "description")
                    .fuzziness("AUTO")
            )._toQuery();
            mustQueries.add(multiMatch);
        }

        if (category != null && !category.trim().isEmpty()) {
            Query termQuery = TermQuery.of(t -> t
                    .field("category")
                    .value(category.trim())
            )._toQuery();
            filterQueries.add(termQuery);
        }

        if (minPrice != null || maxPrice != null) {
            RangeQuery.Builder rangeBuilder = new RangeQuery.Builder().field("price");
            if (minPrice != null) rangeBuilder.gte(co.elastic.clients.json.JsonData.of(minPrice));
            if (maxPrice != null) rangeBuilder.lte(co.elastic.clients.json.JsonData.of(maxPrice));
            filterQueries.add(rangeBuilder.build()._toQuery());
        }

        Query boolQuery = BoolQuery.of(b -> {
            if (!mustQueries.isEmpty()) b.must(mustQueries);
            if (!filterQueries.isEmpty()) b.filter(filterQueries);
            if (mustQueries.isEmpty() && filterQueries.isEmpty()) {
                b.must(Query.of(q -> q.matchAll(m -> m)));
            }
            return b;
        })._toQuery();

        String sortField = sortField(sortBy);
        SortOrder order = "asc".equalsIgnoreCase(sortDirection) ? SortOrder.Asc : SortOrder.Desc;

        var searchQuery = NativeQuery.builder()
                .withQuery(boolQuery)
                .withSort(sort -> sort.field(f -> f.field(sortField).order(order)))
                .withPageable(PageRequest.of(page, size))
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(searchQuery, ProductDocument.class);

        List<ProductResponse> products = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        long total = hits.getTotalHits();

        return ProductListResponse.builder()
                .products(products)
                .totalElements(total)
                .totalPages((int) (total + size - 1) / size)
                .currentPage(page + 1)
                .pageSize(size)
                .hasNext((page + 1) * size < total)
                .hasPrevious(page > 0)
                .build();
    }

    private ProductResponse toProductResponse(ProductDocument doc) {
        Product product = productRepository.findById(Long.parseLong(doc.getId()))
                .orElse(null);
        if (product != null) {
            return ProductResponse.from(product);
        }
        return ProductResponse.builder()
                .id(Long.parseLong(doc.getId()))
                .name(doc.getName())
                .description(doc.getDescription())
                .price(doc.getPrice())
                .stock(0)
                .category(doc.getCategory())
                .imageUrl(doc.getImageUrl())
                .averageRating(doc.getAverageRating())
                .reviewCount(doc.getReviewCount())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getCreatedAt())
                .build();
    }

    private String sortField(String sortBy) {
        if (sortBy == null) return "createdAt";
        return switch (sortBy.toLowerCase()) {
            case "popular", "reviews" -> "reviewCount";
            case "rating" -> "averageRating";
            case "price" -> "price";
            case "recent", "created" -> "createdAt";
            default -> "createdAt";
        };
    }

    private String mapSortBy(String sortBy) {
        if (sortBy == null) return "created";
        return switch (sortBy.toLowerCase()) {
            case "popular", "reviews" -> "reviews";
            case "rating" -> "rating";
            case "price" -> "price";
            case "recent", "created" -> "created";
            default -> "created";
        };
    }
}
