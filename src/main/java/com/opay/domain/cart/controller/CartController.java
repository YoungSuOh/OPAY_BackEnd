package com.opay.domain.cart.controller;

import com.opay.domain.cart.dto.CartListResponse;
import com.opay.domain.cart.dto.CartRequest;
import com.opay.domain.cart.dto.CartResponse;
import com.opay.domain.cart.service.CartService;
import com.opay.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Cart Controller
 * 장바구니 관련 REST API 엔드포인트를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 장바구니에 상품 추가
     * POST /api/carts
     * - 인증된 사용자만 장바구니에 추가 가능
     * - 동일 상품 재추가 시 수량 증가
     */
    @PostMapping
    public ResponseEntity<CartResponse> addToCart(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody CartRequest request) {
        
        Long userId = getUserIdFromToken(token);
        CartResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 장바구니 목록 조회
     * GET /api/carts
     * - 인증된 사용자만 자신의 장바구니 조회 가능
     */
    @GetMapping
    public ResponseEntity<CartListResponse> getCartItems(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        CartListResponse response = cartService.getCartItems(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 장바구니 수량 수정
     * PATCH /api/carts/{id}/quantity
     * - 인증된 사용자만 수정 가능
     * - 본인의 장바구니만 수정 가능
     */
    @PatchMapping("/{id}/quantity")
    public ResponseEntity<CartResponse> updateQuantity(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        
        Long userId = getUserIdFromToken(token);
        CartResponse response = cartService.updateQuantity(id, userId, quantity);
        return ResponseEntity.ok(response);
    }

    /**
     * 장바구니 항목 삭제
     * DELETE /api/carts/{id}
     * - 인증된 사용자만 삭제 가능
     * - 본인의 장바구니만 삭제 가능
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromCart(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        
        Long userId = getUserIdFromToken(token);
        cartService.removeFromCart(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 장바구니 전체 비우기
     * DELETE /api/carts
     * - 인증된 사용자만 전체 비우기 가능
     * - 주문 완료 후 장바구니를 비울 때 사용
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 장바구니 항목 개수 조회
     * GET /api/carts/count
     * - 인증된 사용자만 조회 가능
     * - 장바구니 아이콘에 표시할 항목 수를 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getCartItemCount(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        long count = cartService.getCartItemCount(userId);
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
