package com.opay.admin.service;

import com.opay.admin.dto.DashboardResponse;
import com.opay.domain.order.repository.OrderRepository;
import com.opay.domain.payment.entity.Payment;
import com.opay.domain.payment.repository.PaymentRepository;
import com.opay.domain.product.repository.ProductRepository;
import com.opay.domain.refund.entity.RefundRequest;
import com.opay.domain.refund.repository.RefundRequestRepository;
import com.opay.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRequestRepository refundRequestRepository;
    
    public DashboardResponse getStats() {
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long totalPaymentsSuccess = paymentRepository.countByStatus(Payment.PaymentStatus.SUCCESS);
        long pendingRefundCount = refundRequestRepository.countByStatus(RefundRequest.RefundStatus.PENDING);
        return DashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalPaymentsSuccess(totalPaymentsSuccess)
                .pendingRefundCount(pendingRefundCount)
                .build();
    }
}
