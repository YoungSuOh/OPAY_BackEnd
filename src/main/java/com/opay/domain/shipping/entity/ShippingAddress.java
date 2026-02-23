package com.opay.domain.shipping.entity;

import com.opay.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50)
    private String name; // 배송지 이름 (예: 집, 회사)

    @Column(nullable = false, length = 50)
    private String recipient; // 수령인

    @Column(nullable = false, length = 20)
    private String phone; // 연락처

    @Column(nullable = false, length = 255)
    private String address; // 주소

    @Column(length = 255)
    private String detailAddress; // 상세주소

    @Column(length = 10)
    private String postalCode; // 우편번호

    @Column(nullable = false)
    private Boolean isDefault = false; // 기본 배송지 여부

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ShippingAddress(User user, String name, String recipient, String phone, 
                          String address, String detailAddress, String postalCode, Boolean isDefault, LocalDateTime createdAt) {
        this.user = user;
        this.name = name;
        this.recipient = recipient;
        this.phone = phone;
        this.address = address;
        this.detailAddress = detailAddress;
        this.postalCode = postalCode;
        this.isDefault = isDefault != null ? isDefault : false;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public void update(String name, String recipient, String phone, 
                      String address, String detailAddress, String postalCode) {
        this.name = name;
        this.recipient = recipient;
        this.phone = phone;
        this.address = address;
        this.detailAddress = detailAddress;
        this.postalCode = postalCode;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
