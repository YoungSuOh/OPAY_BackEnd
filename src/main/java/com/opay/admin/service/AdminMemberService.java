package com.opay.admin.service;

import com.opay.admin.dto.AdminMemberDetailResponse;
import com.opay.admin.dto.AdminMemberListResponse;
import com.opay.admin.dto.AdminMemberResponse;
import com.opay.admin.dto.AdminMemberUpdateRequest;
import com.opay.domain.order.repository.OrderRepository;
import com.opay.domain.user.entity.User;
import com.opay.domain.user.repository.UserRepository;
import com.opay.domain.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final WalletService walletService;

    public AdminMemberListResponse getMembers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = keyword != null && !keyword.trim().isEmpty()
                ? userRepository.findAllByKeyword(keyword.trim(), pageable)
                : userRepository.findAll(pageable);

        return AdminMemberListResponse.builder()
                .members(userPage.getContent().stream().map(AdminMemberResponse::from).toList())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .currentPage(userPage.getNumber() + 1)
                .pageSize(userPage.getSize())
                .hasNext(userPage.hasNext())
                .hasPrevious(userPage.hasPrevious())
                .build();
    }

    public AdminMemberDetailResponse getMemberDetail(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + id));
        Long walletBalance = walletService.getBalance(user.getId());
        long orderCount = orderRepository.countByUserId(user.getId());
        return AdminMemberDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .walletBalance(walletBalance)
                .orderCount(orderCount)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public AdminMemberResponse updateMember(Long id, AdminMemberUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + id));
        user.updateProfile(
                request.getName() != null ? request.getName() : user.getName(),
                request.getPhone() != null ? request.getPhone() : user.getPhone());
        return AdminMemberResponse.from(user);
    }
}
