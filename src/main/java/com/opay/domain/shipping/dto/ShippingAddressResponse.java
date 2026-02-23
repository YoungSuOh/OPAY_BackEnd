package com.opay.domain.shipping.dto;

import com.opay.domain.shipping.entity.ShippingAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddressResponse {
    private Long id;
    private String name;
    private String recipient;
    private String phone;
    private String address;
    private String detailAddress;
    private String postalCode;
    private Boolean isDefault;
    private LocalDateTime createdAt;

    public static ShippingAddressResponse from(ShippingAddress shippingAddress) {
        return ShippingAddressResponse.builder()
                .id(shippingAddress.getId())
                .name(shippingAddress.getName())
                .recipient(shippingAddress.getRecipient())
                .phone(shippingAddress.getPhone())
                .address(shippingAddress.getAddress())
                .detailAddress(shippingAddress.getDetailAddress())
                .postalCode(shippingAddress.getPostalCode())
                .isDefault(shippingAddress.getIsDefault())
                .createdAt(shippingAddress.getCreatedAt())
                .build();
    }
}
