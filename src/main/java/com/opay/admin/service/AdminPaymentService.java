package com.opay.admin.service;

import com.opay.admin.dto.AdminPaymentListResponse;
import com.opay.admin.dto.AdminPaymentResponse;
import com.opay.domain.payment.entity.Payment;
import com.opay.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;

    // 괸리자 결제 조회
    public AdminPaymentListResponse getPayments(int page, int size, Long orderId, Long userId, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> paymentPage = (orderId != null || userId != null || (keyword != null && !keyword.trim().isEmpty()))
                ? paymentRepository.findForAdmin(orderId, userId, keyword != null ? keyword.trim() : null, pageable)
                : paymentRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<AdminPaymentResponse> payments = paymentPage.getContent().stream()
                .map(AdminPaymentResponse::from)
                .toList();
        return AdminPaymentListResponse.builder()
                .payments(payments)
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .currentPage(paymentPage.getNumber() + 1)
                .pageSize(paymentPage.getSize())
                .hasNext(paymentPage.hasNext())
                .hasPrevious(paymentPage.hasPrevious())
                .build();
    }

    // 특정 유저 결제 목록 조회
    public AdminPaymentListResponse getPaymentsByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> paymentPage = paymentRepository.findByUserId(userId, pageable);
        List<AdminPaymentResponse> payments = paymentPage.getContent().stream()
                .map(AdminPaymentResponse::from)
                .toList();
        return AdminPaymentListResponse.builder()
                .payments(payments)
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .currentPage(paymentPage.getNumber() + 1)
                .pageSize(paymentPage.getSize())
                .hasNext(paymentPage.hasNext())
                .hasPrevious(paymentPage.hasPrevious())
                .build();
    }
}
