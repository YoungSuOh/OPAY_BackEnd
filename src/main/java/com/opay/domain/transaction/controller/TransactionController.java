package com.opay.domain.transaction.controller;

import com.opay.domain.transaction.dto.TransactionResponse;
import com.opay.domain.transaction.entity.Transaction;
import com.opay.domain.transaction.service.TransactionService;
import com.opay.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transaction Controller
 * 거래 관련 REST API 엔드포인트를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 사용자 거래 목록 조회
     * GET /api/transactions?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        Long userId = getUserIdFromToken(token);
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionService.getTransactionsByUser(userId, pageable);
        
        Page<TransactionResponse> responsePage = transactionPage.map(TransactionResponse::from);
        
        return ResponseEntity.ok(responsePage);
    }

    /**
     * 지갑 거래 목록 조회
     * GET /api/transactions/wallet/{walletId}?page=0&size=20
     */
    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<Page<TransactionResponse>> getTransactionsByWallet(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long walletId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        Long userId = getUserIdFromToken(token);
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionService.getTransactionsByWallet(walletId, pageable);
        
        Page<TransactionResponse> responsePage = transactionPage.map(TransactionResponse::from);
        
        return ResponseEntity.ok(responsePage);
    }

    /**
     * 주문 거래 목록 조회
     * GET /api/transactions/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByOrder(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long orderId) {
        
        Long userId = getUserIdFromToken(token);
        List<Transaction> transactions = transactionService.getTransactionsByOrder(orderId);
        
        List<TransactionResponse> responses = transactions.stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * 결제 거래 목록 조회
     * GET /api/transactions/payment/{paymentId}
     */
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByPayment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long paymentId) {
        
        Long userId = getUserIdFromToken(token);
        List<Transaction> transactions = transactionService.getTransactionsByPayment(paymentId);
        
        List<TransactionResponse> responses = transactions.stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * 기간별 거래 목록 조회
     * GET /api/transactions/date-range?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByDateRange(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        Long userId = getUserIdFromToken(token);
        List<Transaction> transactions = transactionService.getTransactionsByDateRange(
                userId, startDate, endDate);
        
        List<TransactionResponse> responses = transactions.stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
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
