package com.opay.domain.shipping.service;

import com.opay.domain.shipping.dto.ShippingAddressRequest;
import com.opay.domain.shipping.dto.ShippingAddressResponse;
import com.opay.domain.shipping.entity.ShippingAddress;
import com.opay.domain.shipping.repository.ShippingAddressRepository;
import com.opay.domain.user.entity.User;
import com.opay.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingAddressService {

    private final ShippingAddressRepository shippingAddressRepository;
    private final UserRepository userRepository;

    /**
     * 사용자의 배송지 목록 조회
     */
    public List<ShippingAddressResponse> getShippingAddresses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        List<ShippingAddress> addresses = shippingAddressRepository
                .findByUserOrderByIsDefaultDescCreatedAtDesc(user);
        
        return addresses.stream()
                .map(ShippingAddressResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 기본 배송지 조회
     */
    public ShippingAddressResponse getDefaultShippingAddress(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        ShippingAddress defaultAddress = shippingAddressRepository
                .findByUserAndIsDefaultTrue(user)
                .orElse(null);
        
        if (defaultAddress == null) {
            // 기본 배송지가 없으면 첫 번째 배송지 반환
            List<ShippingAddress> addresses = shippingAddressRepository
                    .findByUserOrderByIsDefaultDescCreatedAtDesc(user);
            if (!addresses.isEmpty()) {
                defaultAddress = addresses.get(0);
            } else {
                return null;
            }
        }
        
        return ShippingAddressResponse.from(defaultAddress);
    }

    /**
     * 배송지 추가
     */
    @Transactional
    public ShippingAddressResponse addShippingAddress(Long userId, ShippingAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        // 기본 배송지로 설정하는 경우 기존 기본 배송지 해제
        if (request.getIsDefault() != null && request.getIsDefault()) {
            shippingAddressRepository.clearDefaultByUser(user);
        }
        
        ShippingAddress shippingAddress = ShippingAddress.builder()
                .user(user)
                .name(request.getName())
                .recipient(request.getRecipient())
                .phone(request.getPhone())
                .address(request.getAddress())
                .detailAddress(request.getDetailAddress())
                .postalCode(request.getPostalCode())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        
        ShippingAddress saved = shippingAddressRepository.save(shippingAddress);
        log.info("배송지 추가 완료: userId={}, addressId={}", userId, saved.getId());
        
        return ShippingAddressResponse.from(saved);
    }

    /**
     * 배송지 수정
     */
    @Transactional
    public ShippingAddressResponse updateShippingAddress(Long userId, Long addressId, ShippingAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        ShippingAddress shippingAddress = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("배송지를 찾을 수 없습니다"));
        
        // 다른 사용자의 배송지는 수정 불가
        if (!shippingAddress.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("배송지를 수정할 권한이 없습니다");
        }
        
        // 기본 배송지로 설정하는 경우 기존 기본 배송지 해제
        if (request.getIsDefault() != null && request.getIsDefault() && !shippingAddress.getIsDefault()) {
            shippingAddressRepository.clearDefaultByUser(user);
        }
        
        shippingAddress.update(
                request.getName(),
                request.getRecipient(),
                request.getPhone(),
                request.getAddress(),
                request.getDetailAddress(),
                request.getPostalCode()
        );
        
        if (request.getIsDefault() != null) {
            shippingAddress.setDefault(request.getIsDefault());
        }
        
        log.info("배송지 수정 완료: userId={}, addressId={}", userId, addressId);
        
        return ShippingAddressResponse.from(shippingAddress);
    }

    /**
     * 배송지 삭제
     */
    @Transactional
    public void deleteShippingAddress(Long userId, Long addressId) {
        ShippingAddress shippingAddress = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("배송지를 찾을 수 없습니다"));
        
        // 다른 사용자의 배송지는 삭제 불가
        if (!shippingAddress.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("배송지를 삭제할 권한이 없습니다");
        }
        
        shippingAddressRepository.delete(shippingAddress);
        log.info("배송지 삭제 완료: userId={}, addressId={}", userId, addressId);
    }

    /**
     * 기본 배송지 설정
     */
    @Transactional
    public ShippingAddressResponse setDefaultShippingAddress(Long userId, Long addressId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        ShippingAddress shippingAddress = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("배송지를 찾을 수 없습니다"));
        
        // 다른 사용자의 배송지는 설정 불가
        if (!shippingAddress.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("배송지를 설정할 권한이 없습니다");
        }
        
        // 기존 기본 배송지 해제
        shippingAddressRepository.clearDefaultByUser(user);
        
        // 새로운 기본 배송지 설정
        shippingAddress.setDefault(true);
        
        log.info("기본 배송지 설정 완료: userId={}, addressId={}", userId, addressId);
        
        return ShippingAddressResponse.from(shippingAddress);
    }
}
