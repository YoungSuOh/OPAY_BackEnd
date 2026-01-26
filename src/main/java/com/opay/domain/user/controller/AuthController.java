package com.opay.domain.user.controller;

import com.opay.domain.user.dto.AuthResponse;
import com.opay.domain.user.dto.LoginRequest;
import com.opay.domain.user.dto.SignupRequest;
import com.opay.domain.user.service.UserService;
import com.opay.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletResponse response) {
        
        AuthResponse authResponse = userService.signup(request);
        
        // Refresh Token을 쿠키에 저장
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                authResponse.getUserId(), 
                authResponse.getEmail()
        );
        
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // HTTPS 환경에서는 true로 변경
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        response.addCookie(refreshTokenCookie);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        
        AuthResponse authResponse = userService.login(request);
        
        // Refresh Token을 쿠키에 저장
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                authResponse.getUserId(), 
                authResponse.getEmail()
        );
        
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // HTTPS 환경에서는 true로 변경
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        response.addCookie(refreshTokenCookie);
        
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            String newAccessToken = userService.refreshAccessToken(refreshToken);
            
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            String email = jwtTokenProvider.getEmailFromToken(refreshToken);
            
            // 사용자 정보 조회 (간단히 토큰에서 가져오거나 DB 조회)
            AuthResponse authResponse = AuthResponse.builder()
                    .userId(userId)
                    .email(email)
                    .accessToken(newAccessToken)
                    .build();
            
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Refresh Token 쿠키 삭제
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam(required = false) String email) {
        if (email == null || email.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("available", false);
            response.put("message", "이메일을 입력해주세요");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            boolean isAvailable = !userService.existsByEmail(email.trim());
            Map<String, Object> response = new HashMap<>();
            response.put("available", isAvailable);
            if (!isAvailable) {
                response.put("message", "이미 존재하는 이메일입니다");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("available", false);
            response.put("message", "이메일 중복확인 중 오류가 발생했습니다");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
