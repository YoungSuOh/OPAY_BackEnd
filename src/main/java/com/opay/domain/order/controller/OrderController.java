package com.opay.domain.order.controller;

import com.opay.domain.order.dto.OrderListResponse;
import com.opay.domain.order.dto.OrderRequest;
import com.opay.domain.order.dto.OrderResponse;
import com.opay.domain.order.entity.Order;
import com.opay.domain.order.service.OrderService;
import com.opay.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Order Controller
 * 주문 관련 REST API 엔드포인트를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 주문 생성
     * POST /api/orders
     * - 인증된 사용자만 주문 생성 가능
     * - 장바구니에서 선택한 상품들로 주문 생성
     * - 재고 확인 및 차감, 주문 시점 가격 스냅샷 저장
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody OrderRequest request,
            @RequestParam(required = false, defaultValue = "true") boolean clearCart) {
        
        Long userId = getUserIdFromToken(token);
        OrderResponse response = orderService.createOrder(userId, request, clearCart);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 주문 조회 (단일)
     * GET /api/orders/{id}
     * - 인증된 사용자만 조회 가능
     * - 본인의 주문만 조회 가능
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        
        Long userId = getUserIdFromToken(token);
        OrderResponse response = orderService.getOrder(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자의 주문 목록 조회
     * GET /api/orders
     * - 인증된 사용자만 조회 가능
     * - 본인의 주문 목록만 조회 가능
     * - 페이지네이션 지원
     */
    @GetMapping
    public ResponseEntity<OrderListResponse> getOrders(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        Long userId = getUserIdFromToken(token);
        OrderListResponse response = orderService.getOrdersByUser(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 주문 상태 업데이트
     * PATCH /api/orders/{id}/status
     * - 인증된 사용자만 수정 가능
     * - 본인의 주문만 수정 가능
     * - 배송 상태 추적에 사용
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status) {
        
        Long userId = getUserIdFromToken(token);
        OrderResponse response = orderService.updateOrderStatus(id, userId, status);
        return ResponseEntity.ok(response);
    }

    /**
     * 주문 취소
     * DELETE /api/orders/{id}
     * - 인증된 사용자만 취소 가능
     * - 본인의 주문만 취소 가능
     * - 재고 복구 처리
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        
        Long userId = getUserIdFromToken(token);
        orderService.cancelOrder(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자의 주문 개수 조회
     * GET /api/orders/count
     * - 인증된 사용자만 조회 가능
     * - 전체 주문 개수 반환
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getTotalOrderCount(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        long count = orderService.getTotalOrderCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * 사용자의 이달 주문 개수 조회
     * GET /api/orders/count/monthly
     * - 인증된 사용자만 조회 가능
     * - 현재 달의 주문 개수 반환
     */
    @GetMapping("/count/monthly")
    public ResponseEntity<Long> getMonthlyOrderCount(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        long count = orderService.getMonthlyOrderCount(userId);
        return ResponseEntity.ok(count);
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
