package com.opay.domain.order.service;

import com.opay.domain.cart.entity.Cart;
import com.opay.domain.cart.repository.CartRepository;
import com.opay.domain.order.dto.OrderItemResponse;
import com.opay.domain.order.dto.OrderListResponse;
import com.opay.domain.order.dto.OrderRequest;
import com.opay.domain.order.dto.OrderResponse;
import com.opay.domain.order.entity.Order;
import com.opay.domain.order.repository.OrderRepository;
import com.opay.domain.orderitem.entity.OrderItem;
import com.opay.domain.orderitem.repository.OrderItemRepository;
import com.opay.domain.product.entity.Product;
import com.opay.domain.product.repository.ProductRepository;
import com.opay.domain.user.entity.User;
import com.opay.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OrderItemInfo
 * 주문 상품 정보를 임시로 저장하는 내부 클래스
 */
class OrderItemInfo {
    Product product;
    Integer quantity;
    Long price;

    OrderItemInfo(Product product, Integer quantity, Long price) {
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }
}

/**
 * Order Service
 * 주문 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    /**
     * 주문 생성
     * - 장바구니에서 선택한 상품들로 주문 생성
     * - 재고 확인: 주문 수량이 재고를 초과하지 않는지 확인
     * - 주문 시점의 상품 가격을 스냅샷으로 저장
     * - 주문 생성 후 재고 차감
     * - 주문 생성 후 장바구니에서 해당 상품 제거 (선택적)
     */
    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request, boolean clearCart) {
        // 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 주문 총 금액 계산 및 주문 상품 정보 수집
        // 먼저 상품 정보를 확인하고 주문 총 금액을 계산
        List<OrderItemInfo> orderItemInfos = request.getItems().stream()
                .map(itemRequest -> {
                    // 상품 존재 여부 확인
                    Product product = productRepository.findById(itemRequest.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "상품을 찾을 수 없습니다: " + itemRequest.getProductId()));

                    // 재고 확인: 주문 수량이 재고를 초과하지 않는지 확인
                    if (product.getStock() < itemRequest.getQuantity()) {
                        throw new IllegalArgumentException(
                                "재고가 부족합니다. 상품: " + product.getName() + 
                                ", 현재 재고: " + product.getStock() + 
                                ", 주문 수량: " + itemRequest.getQuantity());
                    }

                    // 주문 시점 가격 저장 (스냅샷)
                    Long orderPrice = product.getPrice();

                    return new OrderItemInfo(product, itemRequest.getQuantity(), orderPrice);
                })
                .collect(Collectors.toList());

        // 주문 총 금액 계산: 모든 주문 상품의 (가격 * 수량) 합계
        long totalAmount = orderItemInfos.stream()
                .mapToLong(info -> info.price * info.quantity)
                .sum();

        // 주문 생성
        Order order = Order.builder()
                .user(user)
                .totalAmount(totalAmount)
                .paidAmount(0L)
                .status(Order.OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        // 주문 상품 생성 및 저장
        // 재고 차감과 함께 OrderItem 생성
        List<OrderItem> savedOrderItems = orderItemInfos.stream()
                .map(info -> {
                    // 재고 차감
                    info.product.updateStock(-info.quantity);

                    // 주문 상품 생성
                    return OrderItem.builder()
                            .user(user)
                            .order(savedOrder)
                            .product(info.product)
                            .quantity(info.quantity)
                            .price(info.price)
                            .build();
                })
                .collect(Collectors.toList());

        savedOrderItems = orderItemRepository.saveAll(savedOrderItems);
        log.info("주문 생성 완료: orderId={}, userId={}, totalAmount={}", 
                savedOrder.getId(), userId, totalAmount);

        // 장바구니에서 주문한 상품 제거 (선택적)
        if (clearCart) {
            request.getItems().forEach(itemRequest -> {
                Cart cart = cartRepository.findByUserIdAndProductId(userId, itemRequest.getProductId())
                        .orElse(null);
                if (cart != null) {
                    cartRepository.delete(cart);
                }
            });
            log.info("주문 후 장바구니 정리 완료: userId={}", userId);
        }

        // 응답 DTO 생성
        List<OrderItemResponse> itemResponses = savedOrderItems.stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList());

        return OrderResponse.from(savedOrder, itemResponses);
    }

    /**
     * 주문 조회 (단일)
     * - 특정 주문의 상세 정보를 조회
     * - 주문에 포함된 모든 상품 정보도 함께 조회
     */
    public OrderResponse getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        // 본인의 주문인지 확인
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 주문만 조회할 수 있습니다");
        }

        // 주문 상품 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList());

        return OrderResponse.from(order, itemResponses);
    }

    /**
     * 사용자의 주문 목록 조회
     * - 특정 사용자의 모든 주문을 페이지네이션과 함께 조회
     * - 최신순으로 정렬
     */
    public OrderListResponse getOrdersByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<OrderResponse> orders = orderPage.getContent().stream()
                .map(order -> {
                    List<OrderItem> orderItems = orderItemRepository
                            .findByOrderIdOrderByCreatedAtAsc(order.getId());
                    List<OrderItemResponse> itemResponses = orderItems.stream()
                            .map(OrderItemResponse::from)
                            .collect(Collectors.toList());
                    return OrderResponse.from(order, itemResponses);
                })
                .collect(Collectors.toList());

        return OrderListResponse.builder()
                .orders(orders)
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .currentPage(orderPage.getNumber() + 1)
                .pageSize(orderPage.getSize())
                .hasNext(orderPage.hasNext())
                .hasPrevious(orderPage.hasPrevious())
                .build();
    }

    /**
     * 주문 상태 업데이트
     * - 주문 상태를 변경 (관리자 또는 시스템에서 사용)
     * - 배송 상태 추적에 사용
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, Long userId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        // 본인의 주문인지 확인
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 주문만 수정할 수 있습니다");
        }

        // 주문 상태 업데이트
        order.updateStatus(status);
        log.info("주문 상태 업데이트: orderId={}, status={}", orderId, status);

        // 주문 상품 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList());

        return OrderResponse.from(order, itemResponses);
    }

    /**
     * 주문 취소
     * - 주문을 취소하고 재고를 복구
     * - 배송 완료된 주문은 취소 불가
     */
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        // 본인의 주문인지 확인
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 주문만 취소할 수 있습니다");
        }

        // 주문 취소
        order.cancel();

        // 주문 상품 조회 및 재고 복구
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        orderItems.forEach(orderItem -> {
            Product product = orderItem.getProduct();
            product.updateStock(orderItem.getQuantity()); // 재고 복구
        });

        log.info("주문 취소 완료: orderId={}, userId={}", orderId, userId);
    }

    /**
     * 사용자의 주문 개수 조회
     * - 전체 주문 개수와 이달의 주문 개수를 조회
     */
    public long getTotalOrderCount(Long userId) {
        return orderRepository.countByUserId(userId);
    }

    /**
     * 사용자의 이달 주문 개수 조회
     * - 현재 달의 주문 개수를 조회
     */
    public long getMonthlyOrderCount(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59);

        return orderRepository.countByUserIdAndCreatedAtBetween(userId, startOfMonth, endOfMonth);
    }
}
