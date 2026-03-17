package com.opay.domain.refund.repository;

import com.opay.domain.refund.entity.RefundRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    Page<RefundRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<RefundRequest> findByStatusOrderByCreatedAtDesc(RefundRequest.RefundStatus status, Pageable pageable);

    long countByStatus(RefundRequest.RefundStatus status);
}
