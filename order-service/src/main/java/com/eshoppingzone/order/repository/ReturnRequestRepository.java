package com.eshoppingzone.order.repository;

import com.eshoppingzone.order.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    List<ReturnRequest> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    List<ReturnRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<ReturnRequest> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<ReturnRequest> findByMerchantIdAndStatusOrderByCreatedAtDesc(Long merchantId, String status);
}
