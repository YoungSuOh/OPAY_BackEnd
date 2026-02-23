package com.opay.domain.wallet.controller;

import com.opay.domain.wallet.dto.WalletChargeRequest;
import com.opay.domain.wallet.dto.WalletResponse;
import com.opay.domain.wallet.service.WalletService;
import com.opay.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Wallet Controller
 * 지갑 관련 REST API 엔드포인트를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 지갑 조회
     * GET /api/wallets
     */
    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        var wallet = walletService.getOrCreateWallet(userId);
        
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    /**
     * 잔액 조회
     * GET /api/wallets/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<Long> getBalance(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        Long balance = walletService.getBalance(userId);
        
        return ResponseEntity.ok(balance);
    }

    /**
     * 잔액 충전
     * POST /api/wallets/charge
     */
    @PostMapping("/charge")
    public ResponseEntity<WalletResponse> chargeBalance(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody WalletChargeRequest request) {
        
        Long userId = getUserIdFromToken(token);
        walletService.chargeBalance(userId, request.getAmount());
        
        var wallet = walletService.getOrCreateWallet(userId);
        
        return ResponseEntity.status(HttpStatus.OK)
                .body(WalletResponse.from(wallet));
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
