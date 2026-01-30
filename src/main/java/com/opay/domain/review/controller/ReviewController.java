package com.opay.domain.review.controller;

import com.opay.domain.review.dto.ReviewListResponse;
import com.opay.domain.review.dto.ReviewRequest;
import com.opay.domain.review.dto.ReviewResponse;
import com.opay.domain.review.service.ReviewService;
import com.opay.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Review Controller
 * 리뷰 관련 REST API 엔드포인트를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/reviews")  // context-path가 /api이므로 /api 제거
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 리뷰 작성
     * POST /api/reviews
     * - 인증된 사용자만 리뷰 작성 가능
     * - 한 상품에 대해 하나의 리뷰만 작성 가능 (중복 방지)
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody ReviewRequest request) {
        
        Long userId = getUserIdFromToken(token);
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 리뷰 조회 (단일)
     * GET /api/reviews/{id}
     * - 모든 사용자가 조회 가능
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long id) {
        ReviewResponse response = reviewService.getReview(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 상품별 리뷰 목록 조회
     * GET /api/reviews/product/{productId}
     * - 특정 상품에 대한 모든 리뷰를 페이지네이션과 함께 조회
     * - 최신순으로 정렬
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ReviewListResponse> getReviewsByProduct(
            @PathVariable Long productId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        ReviewListResponse response = reviewService.getReviewsByProduct(productId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자별 리뷰 목록 조회
     * GET /api/reviews/user/{userId}
     * - 특정 사용자가 작성한 모든 리뷰를 페이지네이션과 함께 조회
     * - 최신순으로 정렬
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ReviewListResponse> getReviewsByUser(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        ReviewListResponse response = reviewService.getReviewsByUser(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 리뷰 수정
     * PUT /api/reviews/{id}
     * - 인증된 사용자만 수정 가능
     * - 본인이 작성한 리뷰만 수정 가능
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        
        Long userId = getUserIdFromToken(token);
        ReviewResponse response = reviewService.updateReview(id, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 리뷰 삭제
     * DELETE /api/reviews/{id}
     * - 인증된 사용자만 삭제 가능
     * - 본인이 작성한 리뷰만 삭제 가능
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        
        Long userId = getUserIdFromToken(token);
        reviewService.deleteReview(id, userId);
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
