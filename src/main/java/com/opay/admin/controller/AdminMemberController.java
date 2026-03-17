package com.opay.admin.controller;

import com.opay.admin.dto.AdminMemberDetailResponse;
import com.opay.admin.dto.AdminMemberListResponse;
import com.opay.admin.dto.AdminMemberResponse;
import com.opay.admin.dto.AdminMemberUpdateRequest;
import com.opay.admin.service.AdminMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public ResponseEntity<AdminMemberListResponse> getMembers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(adminMemberService.getMembers(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminMemberDetailResponse> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(adminMemberService.getMemberDetail(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminMemberResponse> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody AdminMemberUpdateRequest request) {
        return ResponseEntity.ok(adminMemberService.updateMember(id, request));
    }
}
