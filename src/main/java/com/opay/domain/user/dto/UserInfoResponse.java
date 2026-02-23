package com.opay.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String email;
    private String name;
    private Long point;  // 포인트 (현재는 0으로 반환, 추후 Point 시스템 추가 시 연동)
    private Long money; // O머니 (Wallet balance)
}
