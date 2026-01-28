package com.opay.domain.recentproduct.service;

import com.opay.domain.product.entity.Product;
import com.opay.domain.product.repository.ProductRepository;
import com.opay.domain.recentproduct.dto.RecentProductResponse;
import com.opay.domain.recentproduct.entity.RecentProduct;
import com.opay.domain.recentproduct.repository.RecentProductRepository;
import com.opay.domain.user.entity.User;
import com.opay.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RecentProduct Service
 * 최근 본 상품 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentProductService {

    private final RecentProductRepository recentProductRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    
    /**
     * 최대 유지 개수: 사용자당 최대 50개의 최근 본 상품을 유지
     */
    private static final int MAX_RECENT_PRODUCTS = 50;

    /**
     * 최근 본 상품 추가
     * - 사용자가 상품 상세 페이지를 조회할 때 호출
     * - 동일 상품이 이미 있으면 조회 시각만 업데이트
     * - 최대 개수를 초과하면 가장 오래된 항목을 삭제
     */
    @Transactional
    public RecentProductResponse addRecentProduct(Long userId, Long productId) {
        // 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 상품 존재 여부 확인
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        // 동일 상품이 이미 최근 본 상품에 있는지 확인
        RecentProduct existingRecentProduct = recentProductRepository
                .findByUserIdAndProductId(userId, productId)
                .orElse(null);

        RecentProduct savedRecentProduct;
        if (existingRecentProduct != null) {
            // 기존 레코드가 있으면 조회 시각만 업데이트
            existingRecentProduct.updateViewedAt();
            savedRecentProduct = existingRecentProduct;
            log.info("최근 본 상품 조회 시각 업데이트: userId={}, productId={}", userId, productId);
        } else {
            // 새로운 레코드 생성
            RecentProduct recentProduct = RecentProduct.builder()
                    .user(user)
                    .product(product)
                    .build();
            savedRecentProduct = recentProductRepository.save(recentProduct);
            log.info("최근 본 상품 추가 완료: userId={}, productId={}", userId, productId);

            // 최대 개수 확인 및 초과 시 오래된 항목 삭제
            long currentCount = recentProductRepository.countByUserId(userId);
            if (currentCount > MAX_RECENT_PRODUCTS) {
                // 가장 오래된 항목 삭제
                recentProductRepository.deleteOldestByUserId(userId);
                log.info("최근 본 상품 최대 개수 초과로 오래된 항목 삭제: userId={}", userId);
            }
        }

        return RecentProductResponse.from(savedRecentProduct);
    }

    /**
     * 사용자의 최근 본 상품 목록 조회
     * - 특정 사용자가 최근 조회한 상품 목록을 최신순으로 조회
     * - 페이지네이션 지원
     */
    public List<RecentProductResponse> getRecentProducts(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<RecentProduct> recentProductPage = recentProductRepository
                .findByUserIdOrderByViewedAtDesc(userId, pageable);

        return recentProductPage.getContent().stream()
                .map(RecentProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 최근 본 상품 목록 조회 (페이지네이션)
     * - 페이지네이션을 지원하는 최근 본 상품 목록 조회
     */
    public List<RecentProductResponse> getRecentProducts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RecentProduct> recentProductPage = recentProductRepository
                .findByUserIdOrderByViewedAtDesc(userId, pageable);

        return recentProductPage.getContent().stream()
                .map(RecentProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 최근 본 상품 삭제
     * - 특정 최근 본 상품 항목을 삭제
     * - 본인의 최근 본 상품만 삭제 가능
     */
    @Transactional
    public void removeRecentProduct(Long recentProductId, Long userId) {
        RecentProduct recentProduct = recentProductRepository.findById(recentProductId)
                .orElseThrow(() -> new IllegalArgumentException("최근 본 상품을 찾을 수 없습니다: " + recentProductId));

        // 본인의 최근 본 상품인지 확인
        if (!recentProduct.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 최근 본 상품만 삭제할 수 있습니다");
        }

        recentProductRepository.delete(recentProduct);
        log.info("최근 본 상품 삭제 완료: recentProductId={}, userId={}", recentProductId, userId);
    }

    /**
     * 최근 본 상품 전체 삭제
     * - 사용자의 모든 최근 본 상품을 삭제
     * - 최근 본 상품 목록을 초기화할 때 사용
     */
    @Transactional
    public void clearRecentProducts(Long userId) {
        recentProductRepository.deleteAllByUserId(userId);
        log.info("최근 본 상품 전체 삭제 완료: userId={}", userId);
    }
}
