package com.opay.domain.wallet.repository;

/**
 * 지갑 버전·잔액 프로젝션
 * 벌크 UPDATE 시 영속 컨텍스트에 엔티티를 올리지 않고 버전/잔액만 조회할 때 사용
 */
public interface WalletVersionBalance {

    Integer getVersion();

    Long getBalance();
}
