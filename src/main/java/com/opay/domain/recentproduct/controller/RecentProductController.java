package com.opay.domain.recentproduct.controller;

import com.opay.domain.recentproduct.dto.RecentProductResponse;
import com.opay.domain.recentproduct.service.RecentProductService;
import com.opay.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RecentProduct Controller
 * 최근 본 상품 관련 REST API 엔드포인트를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/recent-products")  // context-path가 /api이므로 /api 제거
@RequiredArgsConstructor
public class RecentProductController {

    private final RecentProductService recentProductService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 최근 본 상품 추가
     * POST /api/recent-products
     * - 인증된 사용자만 추가 가능
     * - 상품 상세 페이지 조회 시 자동으로 호출
     */
    @PostMapping
    public ResponseEntity<RecentProductResponse> addRecentProduct(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam Long productId) {
        
        Long userId = getUserIdFromToken(token);
        RecentProductResponse response = recentProductService.addRecentProduct(userId, productId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 최근 본 상품 목록 조회 (제한 개수)
     * GET /api/recent-products?limit=10
     * - 인증된 사용자만 조회 가능
     * - 최신순으로 정렬된 최근 본 상품 목록 반환
     */
    @GetMapping
    public ResponseEntity<List<RecentProductResponse>> getRecentProducts(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        
        Long userId = getUserIdFromToken(token);
        List<RecentProductResponse> response = recentProductService.getRecentProducts(userId, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * 최근 본 상품 목록 조회 (페이지네이션)
     * GET /api/recent-products/page?page=0&size=20
     * - 인증된 사용자만 조회 가능
     * - 페이지네이션을 지원하는 최근 본 상품 목록 조회
     */
    @GetMapping("/page")
    public ResponseEntity<List<RecentProductResponse>> getRecentProductsWithPagination(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        Long userId = getUserIdFromToken(token);
        List<RecentProductResponse> response = recentProductService.getRecentProducts(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 최근 본 상품 삭제
     * DELETE /api/recent-products/{id}
     * - 인증된 사용자만 삭제 가능
     * - 본인의 최근 본 상품만 삭제 가능
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeRecentProduct(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        
        Long userId = getUserIdFromToken(token);
        recentProductService.removeRecentProduct(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 최근 본 상품 전체 삭제
     * DELETE /api/recent-products
     * - 인증된 사용자만 전체 삭제 가능
     * - 최근 본 상품 목록을 초기화할 때 사용
     */
    @DeleteMapping
    public ResponseEntity<Void> clearRecentProducts(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        recentProductService.clearRecentProducts(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     * Authorization 헤더에서 토큰을 추출하여 사용자 ID를 반환
     */
    private Long getUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("인증이 필요합니다");
        }
        String jwt = token.substring(7);
        return jwtTokenProvider.getUserIdFromToken(jwt);
    }
}
