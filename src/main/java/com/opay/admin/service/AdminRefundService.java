package com.opay.admin.service;

import com.opay.admin.dto.AdminRefundListResponse;
import com.opay.admin.dto.AdminRefundResponse;
import com.opay.domain.order.service.OrderService;
import com.opay.domain.refund.entity.RefundRequest;
import com.opay.domain.refund.repository.RefundRequestRepository;
import com.opay.domain.user.entity.User;
import com.opay.domain.user.repository.UserRepository;
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
public class AdminRefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    // 환불 요청 목록 조회
    public AdminRefundListResponse getRefunds(RefundRequest.RefundStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RefundRequest> refundPage = status != null
                ? refundRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : refundRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<AdminRefundResponse> refunds = refundPage.getContent().stream()
                .map(AdminRefundResponse::from)
                .toList();
        return AdminRefundListResponse.builder()
                .refunds(refunds)
                .totalElements(refundPage.getTotalElements())
                .totalPages(refundPage.getTotalPages())
                .currentPage(refundPage.getNumber() + 1)
                .pageSize(refundPage.getSize())
                .hasNext(refundPage.hasNext())
                .hasPrevious(refundPage.hasPrevious())
                .build();
    }

    // 대기중 환불 요청 개수 조회
    public long getPendingRefundCount() {
        return refundRequestRepository.countByStatus(RefundRequest.RefundStatus.PENDING);
    }

    // 환불 승인
    @Transactional
    public AdminRefundResponse approveRefund(Long refundId, Long adminUserId) {
        RefundRequest refund = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("환불 요청을 찾을 수 없습니다: " + refundId));
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다"));
        refund.approve(admin);
        orderService.cancelOrderByAdmin(refund.getOrder().getId());
        return AdminRefundResponse.from(refund);
    }

    // 환불 거절
    @Transactional
    public AdminRefundResponse rejectRefund(Long refundId, Long adminUserId) {
        RefundRequest refund = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("환불 요청을 찾을 수 없습니다: " + refundId));
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다"));
        refund.reject(admin);
        return AdminRefundResponse.from(refund);
    }
}
