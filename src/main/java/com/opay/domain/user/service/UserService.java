package com.opay.domain.user.service;

import com.opay.domain.shipping.entity.ShippingAddress;
import com.opay.domain.shipping.repository.ShippingAddressRepository;
import com.opay.domain.user.dto.AuthResponse;
import com.opay.domain.user.dto.LoginRequest;
import com.opay.domain.user.dto.SignupRequest;
import com.opay.domain.user.dto.UserInfoResponse;
import com.opay.domain.user.entity.User;
import com.opay.domain.user.repository.UserRepository;
import com.opay.domain.wallet.service.WalletService;
import com.opay.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ShippingAddressRepository shippingAddressRepository;
    private final WalletService walletService;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .build();

        User savedUser = userRepository.save(user);

        // 배송지 정보가 있으면 기본 배송지로 저장
        if (request.getShippingAddress() != null && !request.getShippingAddress().trim().isEmpty()) {
            ShippingAddress shippingAddress = ShippingAddress.builder()
                    .user(savedUser)
                    .recipient(request.getShippingRecipient() != null ? request.getShippingRecipient() : savedUser.getName())
                    .phone(request.getShippingPhone() != null ? request.getShippingPhone() : savedUser.getPhone())
                    .address(request.getShippingAddress())
                    .detailAddress(request.getShippingDetailAddress())
                    .postalCode(request.getShippingPostalCode())
                    .isDefault(true)
                    .build();
            
            shippingAddressRepository.save(shippingAddress);
            log.info("기본 배송지 저장 완료: userId={}, address={}", savedUser.getId(), request.getShippingAddress());
        }

        String roleStr = savedUser.getRole() != null ? savedUser.getRole().name() : "USER";
        String accessToken = jwtTokenProvider.generateAccessToken(savedUser.getId(), savedUser.getEmail(), roleStr);
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getId(), savedUser.getEmail(), roleStr);

        log.info("회원가입 완료: userId={}, email={}", savedUser.getId(), savedUser.getEmail());

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role(roleStr)
                .accessToken(accessToken)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        String roleStr = user.getRole() != null ? user.getRole().name() : "USER";
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roleStr);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), roleStr);

        log.info("로그인 완료: userId={}, email={}", user.getId(), user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(roleStr)
                .accessToken(accessToken)
                .build();
    }

    public String refreshAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        String roleStr = user.getRole() != null ? user.getRole().name() : "USER";
        return jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roleStr);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 현재 사용자 정보 조회 (포인트, 머니 포함)
     * 
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        // Wallet에서 머니 조회
        Long money = walletService.getBalance(userId);
        
        // 포인트는 현재 0으로 반환 (추후 Point 시스템 추가 시 연동)
        Long point = 0L;
        
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .point(point)
                .money(money)
                .build();
    }
}
