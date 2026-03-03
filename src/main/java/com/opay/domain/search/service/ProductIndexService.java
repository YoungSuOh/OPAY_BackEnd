package com.opay.domain.search.service;

import com.opay.domain.product.entity.Product;
import com.opay.domain.product.repository.ProductRepository;
import com.opay.domain.search.document.ProductDocument;
import com.opay.domain.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void syncAllOnStartup() {
        try {
            log.info("Elasticsearch 인덱스 동기화 시작");
            int pageSize = 500;
            int page = 0;
            long total = 0;

            while (true) {
                var pageable = PageRequest.of(page, pageSize);
                var products = productRepository.findAll(pageable).getContent();
                if (products.isEmpty()) break;

                List<ProductDocument> docs = products.stream()
                        .map(this::toDocument)
                        .collect(Collectors.toList());
                productSearchRepository.saveAll(docs);
                total += docs.size();
                page++;
            }

            log.info("Elasticsearch 인덱스 동기화 완료: {} 건", total);
        } catch (Exception e) {
            log.warn("Elasticsearch 인덱스 동기화 실패 (fallback 사용): {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public void indexProduct(Product product) {
        try {
            ProductDocument doc = toDocument(product);
            productSearchRepository.save(doc);
            log.debug("상품 인덱싱 완료: productId={}", product.getId());
        } catch (Exception e) {
            log.warn("상품 인덱싱 실패: productId={}, error={}", product.getId(), e.getMessage());
        }
    }

    public void deleteFromIndex(Long productId) {
        try {
            productSearchRepository.deleteById(String.valueOf(productId));
            log.debug("상품 인덱스 삭제 완료: productId={}", productId);
        } catch (Exception e) {
            log.warn("상품 인덱스 삭제 실패: productId={}, error={}", productId, e.getMessage());
        }
    }

    private ProductDocument toDocument(Product p) {
        return ProductDocument.builder()
                .id(String.valueOf(p.getId()))
                .name(p.getName())
                .description(p.getDescription() != null ? p.getDescription() : "")
                .category(p.getCategory() != null ? p.getCategory() : "")
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .averageRating(p.getAverageRating() != null ? p.getAverageRating() : 0.0)
                .reviewCount(p.getReviewCount() != null ? p.getReviewCount() : 0)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
