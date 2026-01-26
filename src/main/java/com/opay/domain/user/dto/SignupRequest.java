package com.opay.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    @Pattern(
        regexp = "^(?=.*[!@#$%^&*(),.?\":{}|<>])(?=.*[a-zA-Z0-9]).{8,}$",
        message = "비밀번호는 특수문자를 포함하여 8자 이상이어야 합니다"
    )
    private String password;

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다")
    private String name;

    @Size(max = 20, message = "전화번호는 20자 이하여야 합니다")
    @Pattern(
        regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$|^$",
        message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)"
    )
    private String phone;

    // 배송지 정보 (선택)
    private String shippingRecipient;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingDetailAddress;
    private String shippingPostalCode;
}
