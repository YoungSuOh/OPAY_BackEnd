package com.opay.domain.shipping.controller;

import com.opay.domain.shipping.dto.ShippingAddressRequest;
import com.opay.domain.shipping.dto.ShippingAddressResponse;
import com.opay.domain.shipping.service.ShippingAddressService;
import com.opay.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipping-addresses")
@RequiredArgsConstructor
public class ShippingAddressController {

    private final ShippingAddressService shippingAddressService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 배송지 목록 조회
     * GET /api/shipping-addresses
     */
    @GetMapping
    public ResponseEntity<List<ShippingAddressResponse>> getShippingAddresses(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        List<ShippingAddressResponse> addresses = shippingAddressService.getShippingAddresses(userId);
        
        return ResponseEntity.ok(addresses);
    }

    /**
     * 기본 배송지 조회
     * GET /api/shipping-addresses/default
     */
    @GetMapping("/default")
    public ResponseEntity<ShippingAddressResponse> getDefaultShippingAddress(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        ShippingAddressResponse address = shippingAddressService.getDefaultShippingAddress(userId);
        
        if (address == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(address);
    }

    /**
     * 배송지 추가
     * POST /api/shipping-addresses
     */
    @PostMapping
    public ResponseEntity<ShippingAddressResponse> addShippingAddress(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody ShippingAddressRequest request) {
        
        Long userId = getUserIdFromToken(token);
        ShippingAddressResponse address = shippingAddressService.addShippingAddress(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    /**
     * 배송지 수정
     * PUT /api/shipping-addresses/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ShippingAddressResponse> updateShippingAddress(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody ShippingAddressRequest request) {
        
        Long userId = getUserIdFromToken(token);
        ShippingAddressResponse address = shippingAddressService.updateShippingAddress(userId, id, request);
        
        return ResponseEntity.ok(address);
    }

    /**
     * 배송지 삭제
     * DELETE /api/shipping-addresses/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShippingAddress(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        
        Long userId = getUserIdFromToken(token);
        shippingAddressService.deleteShippingAddress(userId, id);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * 기본 배송지 설정
     * PATCH /api/shipping-addresses/{id}/default
     */
    @PatchMapping("/{id}/default")
    public ResponseEntity<ShippingAddressResponse> setDefaultShippingAddress(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        
        Long userId = getUserIdFromToken(token);
        ShippingAddressResponse address = shippingAddressService.setDefaultShippingAddress(userId, id);
        
        return ResponseEntity.ok(address);
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
