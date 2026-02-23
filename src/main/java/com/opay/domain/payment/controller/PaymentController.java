package com.opay.domain.payment.controller;

import com.opay.domain.payment.dto.PaymentApproveRequest;
import com.opay.domain.payment.dto.PaymentRequest;
import com.opay.domain.payment.dto.PaymentResponse;
import com.opay.domain.payment.entity.Payment;
import com.opay.domain.payment.service.PaymentService;
import com.opay.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Payment Controller
 * 결제 관련 REST API 엔드포인트를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 결제 요청 (Payment 생성)
     * POST /api/payments/request
     * - 멱등성 보장: 동일한 idempotency_key로 중복 생성 방지
     */
    @PostMapping("/request")
    public ResponseEntity<PaymentResponse> requestPayment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody PaymentRequest request) {
        
        Long userId = getUserIdFromToken(token);
        var payment = paymentService.requestPayment(
                request.getOrderId(),
                request.getAmount(),
                request.getMethod(),
                request.getIdempotencyKey()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentResponse.from(payment));
    }

    /**
     * 결제 승인
     * POST /api/payments/approve
     * - 결제 처리 흐름 실행
     * - 멱등성 보장
     */
    @PostMapping("/approve")
    public ResponseEntity<PaymentResponse> approvePayment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody PaymentApproveRequest request) {
        
        Long userId = getUserIdFromToken(token);
        var payment = paymentService.approvePayment(
                request.getPaymentId(),
                request.getIdempotencyKey()
        );
        
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    /**
     * 결제 상태 조회
     * GET /api/payments/{id}/status
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        
        Long userId = getUserIdFromToken(token);
        var payment = paymentService.getPayment(id);
        
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    /**
     * 결제 조회 (단일)
     * GET /api/payments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        
        Long userId = getUserIdFromToken(token);
        var payment = paymentService.getPayment(id);
        
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    /**
     * 주문 결제 목록 조회
     * GET /api/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrder(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long orderId) {
        
        Long userId = getUserIdFromToken(token);
        var payments = paymentService.getPaymentsByOrder(orderId);
        
        List<PaymentResponse> responses = payments.stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * 사용자 결제 목록 조회
     * GET /api/payments?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getPayments(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        Long userId = getUserIdFromToken(token);
        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> paymentPage = paymentService.getPaymentsByUser(userId, pageable);
        
        Page<PaymentResponse> responsePage = paymentPage.map(PaymentResponse::from);
        
        return ResponseEntity.ok(responsePage);
    }

    /**
     * 결제 취소
     * DELETE /api/payments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelPayment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        
        Long userId = getUserIdFromToken(token);
        paymentService.cancelPayment(id);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private Long getUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("인증이 필요합니다");
        }
        String jwt = token.substring(7);
        return jwtTokenProvider.getUserIdFromToken(jwt);
    }
}
