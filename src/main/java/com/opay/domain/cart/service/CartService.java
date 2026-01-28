package com.opay.domain.cart.service;

import com.opay.domain.cart.dto.CartListResponse;
import com.opay.domain.cart.dto.CartRequest;
import com.opay.domain.cart.dto.CartResponse;
import com.opay.domain.cart.entity.Cart;
import com.opay.domain.cart.repository.CartRepository;
import com.opay.domain.product.entity.Product;
import com.opay.domain.product.repository.ProductRepository;
import com.opay.domain.user.entity.User;
import com.opay.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Cart Service
 * 장바구니 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * 장바구니에 상품 추가
     * - 사용자가 상품을 장바구니에 추가
     * - 동일 상품이 이미 장바구니에 있으면 수량을 증가시킴
     * - 재고 확인: 장바구니에 담을 수량이 재고를 초과하지 않는지 확인
     */
    @Transactional
    public CartResponse addToCart(Long userId, CartRequest request) {
        // 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 상품 존재 여부 확인
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + request.getProductId()));

        // 재고 확인: 요청한 수량이 재고를 초과하지 않는지 확인
        if (product.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + product.getStock());
        }

        // 동일 상품이 이미 장바구니에 있는지 확인
        Cart existingCart = cartRepository.findByUserIdAndProductId(userId, request.getProductId())
                .orElse(null);

        Cart savedCart;
        if (existingCart != null) {
            // 기존 장바구니 항목이 있으면 수량 증가
            // 증가 후 수량이 재고를 초과하지 않는지 확인
            int newQuantity = existingCart.getQuantity() + request.getQuantity();
            if (product.getStock() < newQuantity) {
                throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + product.getStock() + 
                        ", 장바구니 수량: " + existingCart.getQuantity());
            }
            existingCart.increaseQuantity(request.getQuantity());
            savedCart = existingCart;
            log.info("장바구니 수량 증가: cartId={}, userId={}, productId={}, quantity={}", 
                    existingCart.getId(), userId, request.getProductId(), existingCart.getQuantity());
        } else {
            // 새로운 장바구니 항목 생성
            Cart cart = Cart.builder()
                    .user(user)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            savedCart = cartRepository.save(cart);
            log.info("장바구니 추가 완료: cartId={}, userId={}, productId={}, quantity={}", 
                    savedCart.getId(), userId, request.getProductId(), request.getQuantity());
        }

        return CartResponse.from(savedCart);
    }

    /**
     * 사용자의 장바구니 목록 조회
     * - 특정 사용자의 모든 장바구니 항목을 조회
     * - 각 항목의 총 가격과 전체 금액을 계산하여 반환
     */
    public CartListResponse getCartItems(Long userId) {
        List<Cart> carts = cartRepository.findByUserIdOrderByAddedAtDesc(userId);

        List<CartResponse> items = carts.stream()
                .map(CartResponse::from)
                .collect(Collectors.toList());

        // 전체 금액 계산: 모든 항목의 (상품 가격 * 수량) 합계
        long totalAmount = items.stream()
                .mapToLong(CartResponse::getTotalPrice)
                .sum();

        return CartListResponse.builder()
                .items(items)
                .totalItems(carts.size())
                .totalAmount(totalAmount)
                .build();
    }

    /**
     * 장바구니 수량 수정
     * - 장바구니에서 상품의 수량을 변경
     * - 재고 확인: 변경할 수량이 재고를 초과하지 않는지 확인
     * - 본인의 장바구니만 수정 가능
     */
    @Transactional
    public CartResponse updateQuantity(Long cartId, Long userId, Integer quantity) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 항목을 찾을 수 없습니다: " + cartId));

        // 본인의 장바구니인지 확인
        if (!cart.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 장바구니만 수정할 수 있습니다");
        }

        // 재고 확인: 변경할 수량이 재고를 초과하지 않는지 확인
        if (cart.getProduct().getStock() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + cart.getProduct().getStock());
        }

        // 수량 업데이트
        cart.updateQuantity(quantity);
        log.info("장바구니 수량 수정 완료: cartId={}, userId={}, quantity={}", cartId, userId, quantity);

        return CartResponse.from(cart);
    }

    /**
     * 장바구니 항목 삭제
     * - 장바구니에서 특정 항목을 제거
     * - 본인의 장바구니만 삭제 가능
     */
    @Transactional
    public void removeFromCart(Long cartId, Long userId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 항목을 찾을 수 없습니다: " + cartId));

        // 본인의 장바구니인지 확인
        if (!cart.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 장바구니만 삭제할 수 있습니다");
        }

        cartRepository.delete(cart);
        log.info("장바구니 항목 삭제 완료: cartId={}, userId={}", cartId, userId);
    }

    /**
     * 장바구니 전체 비우기
     * - 사용자의 모든 장바구니 항목을 삭제
     * - 주문 완료 후 장바구니를 비울 때 사용
     */
    @Transactional
    public void clearCart(Long userId) {
        cartRepository.deleteAllByUserId(userId);
        log.info("장바구니 전체 비우기 완료: userId={}", userId);
    }

    /**
     * 장바구니 항목 개수 조회
     * - 장바구니 아이콘에 표시할 항목 수를 조회
     */
    public long getCartItemCount(Long userId) {
        return cartRepository.countByUserId(userId);
    }
}
