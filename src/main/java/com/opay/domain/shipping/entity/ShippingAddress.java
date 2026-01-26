package com.opay.domain.shipping.entity;

import com.opay.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ShippingAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String recipient;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 255)
    private String detailAddress;

    @Column(length = 20)
    private String postalCode;

    @Column(nullable = false)
    private Boolean isDefault = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ShippingAddress(User user, String recipient, String phone, String address, 
                          String detailAddress, String postalCode, Boolean isDefault) {
        this.user = user;
        this.recipient = recipient;
        this.phone = phone;
        this.address = address;
        this.detailAddress = detailAddress;
        this.postalCode = postalCode;
        this.isDefault = isDefault != null ? isDefault : false;
    }

    public void updateAddress(String recipient, String phone, String address, 
                             String detailAddress, String postalCode) {
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
