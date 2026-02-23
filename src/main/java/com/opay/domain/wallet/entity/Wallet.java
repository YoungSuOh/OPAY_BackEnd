package com.opay.domain.wallet.entity;

import com.opay.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Wallet Entity
 * 사용자의 지갑 정보를 저장하는 엔티티
 * - 잔액 보유 및 관리
 * - 동시성 제어를 위한 version 필드 (Optimistic Locking)
 * - 사용자당 하나의 지갑만 존재 (1:1 관계)
 */
@Entity
@Table(name = "wallets", uniqueConstraints = {
    @UniqueConstraint(name = "uk_wallet_user_id", columnNames = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 지갑 소유자
     * One-to-One 관계: 한 사용자는 하나의 지갑만 가짐
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * 잔액
     * 포인트와 머니를 합친 총 잔액
     */
    @Column(nullable = false)
    private Long balance = 0L;

    /**
     * 낙관적 잠금 버전
     * 동시성 제어를 위해 사용
     * 잔액 변경 시마다 증가
     */
    @Version
    @Column(nullable = false)
    private Integer version = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Wallet(User user, Long balance) {
        this.user = user;
        this.balance = balance != null ? balance : 0L;
    }

    /**
     * 잔액 차감
     * 조건부 업데이트를 위해 사용
     * 
     * @param amount 차감할 금액
     * @return 차감 성공 여부
     */
    public boolean deduct(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다");
        }
        if (this.balance < amount) {
            return false; // 잔액 부족
        }
        this.balance -= amount;
        return true;
    }

    /**
     * 잔액 증가
     * 
     * @param amount 증가할 금액
     */
    public void add(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("증가 금액은 0보다 커야 합니다");
        }
        this.balance += amount;
    }

    /**
     * 잔액 충전
     * 
     * @param amount 충전할 금액
     */
    public void charge(Long amount) {
        add(amount);
    }
}
