package com.opay.domain.refund.controller;

import com.opay.domain.refund.dto.CreateRefundRequestDto;
import com.opay.domain.refund.dto.RefundRequestDto;
import com.opay.domain.refund.entity.RefundRequest;
import com.opay.domain.refund.service.RefundRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/refund-requests")
@RequiredArgsConstructor
public class RefundController {

    private final RefundRequestService refundRequestService;

    @PostMapping
    public ResponseEntity<RefundRequestDto> createRefundRequest(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateRefundRequestDto request) {
        RefundRequest refund = refundRequestService.createRequest(
                request.getOrderId(),
                userId,
                request.getAmount(),
                request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(RefundRequestDto.from(refund));
    }
}
