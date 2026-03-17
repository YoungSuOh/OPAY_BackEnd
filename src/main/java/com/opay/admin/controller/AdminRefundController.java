package com.opay.admin.controller;

import com.opay.admin.dto.AdminRefundListResponse;
import com.opay.admin.dto.AdminRefundResponse;
import com.opay.admin.service.AdminRefundService;
import com.opay.domain.refund.entity.RefundRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final AdminRefundService adminRefundService;

    @GetMapping
    public ResponseEntity<AdminRefundListResponse> getRefunds(
            @RequestParam(required = false) RefundRequest.RefundStatus status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(adminRefundService.getRefunds(status, page, size));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AdminRefundResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminUserId) {
        return ResponseEntity.ok(adminRefundService.approveRefund(id, adminUserId));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AdminRefundResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminUserId) {
        return ResponseEntity.ok(adminRefundService.rejectRefund(id, adminUserId));
    }
}
