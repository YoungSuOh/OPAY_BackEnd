package com.opay.domain.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddressRequest {
    private String name; // 배송지 이름 (선택)
    
    @NotBlank(message = "수령인을 입력해주세요")
    private String recipient;
    
    @NotBlank(message = "연락처를 입력해주세요")
    private String phone;
    
    @NotBlank(message = "주소를 입력해주세요")
    private String address;
    
    private String detailAddress; // 상세주소 (선택)
    
    private String postalCode; // 우편번호 (선택)
    
    private Boolean isDefault = false; // 기본 배송지 여부
}
