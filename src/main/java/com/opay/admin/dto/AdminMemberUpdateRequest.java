package com.opay.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class AdminMemberUpdateRequest {
    @Size(max = 50)
    private String name;
    @Size(max = 20)
    private String phone;
}
