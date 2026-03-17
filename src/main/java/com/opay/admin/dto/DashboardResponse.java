package com.opay.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private long totalPaymentsSuccess;
    private long pendingRefundCount;  // 추후 RefundRequest 연동 시 사용
}
