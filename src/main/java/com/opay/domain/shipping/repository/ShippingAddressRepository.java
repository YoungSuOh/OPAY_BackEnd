package com.opay.domain.shipping.repository;

import com.opay.domain.shipping.entity.ShippingAddress;
import com.opay.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {
    List<ShippingAddress> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);
    Optional<ShippingAddress> findByUserAndIsDefaultTrue(User user);
    
    @Modifying
    @Query("UPDATE ShippingAddress sa SET sa.isDefault = false WHERE sa.user = :user")
    void clearDefaultByUser(@Param("user") User user);
}
