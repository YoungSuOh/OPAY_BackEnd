package com.opay.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMemberDetailResponse {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private String role;
    private Long walletBalance;
    private long orderCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
