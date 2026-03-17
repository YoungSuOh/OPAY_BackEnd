package com.opay.admin.controller;

import com.opay.admin.dto.AdminPaymentListResponse;
import com.opay.admin.service.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @GetMapping
    public ResponseEntity<AdminPaymentListResponse> getPayments(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(adminPaymentService.getPayments(page, size, orderId, userId, keyword));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<AdminPaymentListResponse> getPaymentsByUser(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(adminPaymentService.getPaymentsByUserId(userId, page, size));
    }
}
