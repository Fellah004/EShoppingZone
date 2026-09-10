package com.eshoppingzone.order.repository;

import com.eshoppingzone.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    List<Order> findByDeliveryAgentIdOrderByOrderDateDesc(Long deliveryAgentId);

    List<Order> findAllByOrderByOrderDateDesc();

    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.merchantId = :merchantId ORDER BY o.orderDate DESC")
    List<Order> findOrdersByMerchantId(@Param("merchantId") Long merchantId);
}
