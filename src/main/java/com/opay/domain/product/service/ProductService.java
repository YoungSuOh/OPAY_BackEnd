package com.opay.domain.product.service;

import com.opay.domain.product.dto.ProductListResponse;
import com.opay.domain.product.dto.ProductRequest;
import com.opay.domain.product.dto.ProductResponse;
import com.opay.domain.product.dto.ProductUpdateRequest;
import com.opay.domain.product.entity.Product;
import com.opay.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("상품 생성 완료: productId={}, name={}", savedProduct.getId(), savedProduct.getName());

        return ProductResponse.from(savedProduct);
    }

    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + id));

        return ProductResponse.from(product);
    }

    public ProductListResponse getProducts(
            String keyword,
            String category,
            Long minPrice,
            Long maxPrice,
            String sortBy,
            String sortDirection,
            int page,
            int size) {

        Pageable pageable = createPageable(sortBy, sortDirection, page, size);
        Page<Product> productPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            if (category != null && !category.trim().isEmpty()) {
                productPage = productRepository.searchByCategoryAndKeyword(category, keyword.trim(), pageable);
            } else {
                productPage = productRepository.searchByKeyword(keyword.trim(), pageable);
            }
        } else if (category != null && !category.trim().isEmpty()) {
            productPage = productRepository.findByCategory(category.trim(), pageable);
        } else if (minPrice != null && maxPrice != null) {
            productPage = productRepository.findByPriceBetween(minPrice, maxPrice, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        List<ProductResponse> products = productPage.getContent().stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());

        return ProductListResponse.builder()
                .products(products)
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .currentPage(productPage.getNumber() + 1)
                .pageSize(productPage.getSize())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();
    }

    public List<String> getCategories() {
        return productRepository.findAllCategories();
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + id));

        product.update(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getStock(),
                request.getCategory(),
                request.getImageUrl()
        );

        log.info("상품 수정 완료: productId={}, name={}", product.getId(), product.getName());

        return ProductResponse.from(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + id));

        productRepository.delete(product);
        log.info("상품 삭제 완료: productId={}, name={}", product.getId(), product.getName());
    }

    @Transactional
    public ProductResponse updateStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + id));

        product.updateStock(quantity);
        log.info("상품 재고 수정 완료: productId={}, quantity={}", product.getId(), quantity);

        return ProductResponse.from(product);
    }

    private Pageable createPageable(String sortBy, String sortDirection, int page, int size) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;

        Sort sort;
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            switch (sortBy.toLowerCase()) {
                case "price":
                    sort = Sort.by(direction, "price");
                    break;
                case "name":
                    sort = Sort.by(direction, "name");
                    break;
                case "rating":
                    sort = Sort.by(direction, "averageRating");
                    break;
                case "reviews":
                    sort = Sort.by(direction, "reviewCount");
                    break;
                case "created":
                    sort = Sort.by(direction, "createdAt");
                    break;
                default:
                    sort = Sort.by(Sort.Direction.DESC, "createdAt");
            }
        } else {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return PageRequest.of(page, size, sort);
    }
}
