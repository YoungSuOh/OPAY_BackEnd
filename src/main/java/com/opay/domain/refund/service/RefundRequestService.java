package com.opay.domain.refund.service;

import com.opay.domain.order.entity.Order;
import com.opay.domain.order.repository.OrderRepository;
import com.opay.domain.payment.entity.Payment;
import com.opay.domain.payment.repository.PaymentRepository;
import com.opay.domain.refund.entity.RefundRequest;
import com.opay.domain.refund.repository.RefundRequestRepository;
import com.opay.domain.user.entity.User;
import com.opay.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundRequestService {

    private final RefundRequestRepository refundRequestRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Transactional
    public RefundRequest createRequest(Long orderId, Long userId, Long amount, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 주문만 환불 요청할 수 있습니다");
        }
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문입니다");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        List<Payment> successPayments = paymentRepository.findByOrderIdAndStatus(orderId, Payment.PaymentStatus.SUCCESS);
        Payment payment = successPayments.isEmpty() ? null : successPayments.get(0);
        RefundRequest refund = RefundRequest.builder()
                .order(order)
                .payment(payment)
                .amount(amount != null ? amount : order.getPaidAmount())
                .reason(reason)
                .requestedBy(user)
                .build();
        return refundRequestRepository.save(refund);
    }
}
