package com.eshoppingzone.order.repository;

import com.eshoppingzone.order.entity.CustomerComplaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerComplaintRepository extends JpaRepository<CustomerComplaint, Long> {

    List<CustomerComplaint> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<CustomerComplaint> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<CustomerComplaint> findAllByOrderByCreatedAtDesc();

    List<CustomerComplaint> findByStatusOrderByCreatedAtDesc(String status);
}
