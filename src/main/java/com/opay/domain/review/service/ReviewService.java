package com.opay.domain.review.service;

import com.opay.domain.product.entity.Product;
import com.opay.domain.product.repository.ProductRepository;
import com.opay.domain.review.dto.ReviewListResponse;
import com.opay.domain.review.dto.ReviewRequest;
import com.opay.domain.review.dto.ReviewResponse;
import com.opay.domain.review.entity.Review;
import com.opay.domain.review.repository.ReviewRepository;
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
 * Review Service
 * 리뷰 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * 리뷰 작성
     * - 사용자가 상품에 대한 리뷰를 작성
     * - 한 사용자는 한 상품에 대해 하나의 리뷰만 작성 가능 (중복 방지)
     * - 리뷰 작성 후 상품의 평균 평점과 리뷰 수를 업데이트
     */
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        // 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 상품 존재 여부 확인
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + request.getProductId()));

        // 중복 리뷰 작성 방지: 이미 해당 상품에 리뷰를 작성했는지 확인
        if (reviewRepository.findByUserIdAndProductId(userId, request.getProductId()).isPresent()) {
            throw new IllegalArgumentException("이미 해당 상품에 리뷰를 작성하셨습니다");
        }

        // 리뷰 생성 및 저장
        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("리뷰 작성 완료: reviewId={}, userId={}, productId={}", 
                savedReview.getId(), userId, request.getProductId());

        // 상품의 평균 평점과 리뷰 수 업데이트
        updateProductRating(request.getProductId());

        return ReviewResponse.from(savedReview);
    }

    /**
     * 리뷰 조회 (단일)
     * - 특정 리뷰의 상세 정보를 조회
     */
    public ReviewResponse getReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다: " + id));

        return ReviewResponse.from(review);
    }

    /**
     * 상품별 리뷰 목록 조회
     * - 특정 상품에 대한 모든 리뷰를 페이지네이션과 함께 조회
     * - 최신순으로 정렬하여 반환
     */
    public ReviewListResponse getReviewsByProduct(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);

        List<ReviewResponse> reviews = reviewPage.getContent().stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());

        return ReviewListResponse.builder()
                .reviews(reviews)
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .currentPage(reviewPage.getNumber() + 1)
                .pageSize(reviewPage.getSize())
                .hasNext(reviewPage.hasNext())
                .hasPrevious(reviewPage.hasPrevious())
                .build();
    }

    /**
     * 사용자별 리뷰 목록 조회
     * - 특정 사용자가 작성한 모든 리뷰를 페이지네이션과 함께 조회
     * - 마이페이지에서 내가 작성한 리뷰를 조회할 때 사용
     */
    public ReviewListResponse getReviewsByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<ReviewResponse> reviews = reviewPage.getContent().stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());

        return ReviewListResponse.builder()
                .reviews(reviews)
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .currentPage(reviewPage.getNumber() + 1)
                .pageSize(reviewPage.getSize())
                .hasNext(reviewPage.hasNext())
                .hasPrevious(reviewPage.hasPrevious())
                .build();
    }

    /**
     * 리뷰 수정
     * - 리뷰 작성자만 자신의 리뷰를 수정할 수 있음
     * - 평점과 내용을 수정할 수 있으며, 수정 후 상품의 평균 평점을 재계산
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, Long userId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다: " + reviewId));

        // 리뷰 작성자 확인: 본인이 작성한 리뷰만 수정 가능
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인이 작성한 리뷰만 수정할 수 있습니다");
        }

        // 리뷰 수정
        review.update(request.getRating(), request.getContent());
        log.info("리뷰 수정 완료: reviewId={}, userId={}", reviewId, userId);

        // 상품의 평균 평점 재계산
        updateProductRating(review.getProduct().getId());

        return ReviewResponse.from(review);
    }

    /**
     * 리뷰 삭제
     * - 리뷰 작성자만 자신의 리뷰를 삭제할 수 있음
     * - 삭제 후 상품의 평균 평점과 리뷰 수를 업데이트
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다: " + reviewId));

        // 리뷰 작성자 확인: 본인이 작성한 리뷰만 삭제 가능
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인이 작성한 리뷰만 삭제할 수 있습니다");
        }

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        log.info("리뷰 삭제 완료: reviewId={}, userId={}", reviewId, userId);

        // 상품의 평균 평점과 리뷰 수 업데이트
        updateProductRating(productId);
    }

    /**
     * 상품의 평균 평점과 리뷰 수 업데이트
     * - 리뷰 작성/수정/삭제 시 호출되어 상품의 평균 평점과 리뷰 수를 재계산
     * - 상품의 평균 평점은 모든 리뷰의 평점을 평균낸 값
     */
    @Transactional
    private void updateProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        // 평균 평점 계산
        Double averageRating = reviewRepository.calculateAverageRatingByProductId(productId);
        if (averageRating == null) {
            averageRating = 0.0;
        }

        // 리뷰 수 조회
        long reviewCount = reviewRepository.countByProductId(productId);

        // 상품의 평균 평점과 리뷰 수 업데이트
        product.updateRating(averageRating, (int) reviewCount);
        log.info("상품 평균 평점 업데이트: productId={}, averageRating={}, reviewCount={}", 
                productId, averageRating, reviewCount);
    }
}
