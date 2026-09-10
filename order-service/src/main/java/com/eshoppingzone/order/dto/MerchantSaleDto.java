package com.eshoppingzone.order.dto;

import com.eshoppingzone.order.entity.Address;
import com.eshoppingzone.order.entity.OrderStatus;
import com.eshoppingzone.order.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantSaleDto {

    private Long orderId;
    private Long productId;
    private Long merchantId;
    private String productName;
    private Double price;
    private Integer quantity;
    private Double subTotal;
    private String imageUrl;
    private Long customerId;
    private String customerName;
    private PaymentMethod paymentMethod;
    private String paymentStatus;
    private OrderStatus orderStatus;
    private LocalDateTime orderDate;
    private Address shippingAddress;
    private Long deliveryAgentId;
    private String deliveryAgentName;

    public MerchantSaleDto(Long orderId, Long productId, Long merchantId, String productName, Double price, Integer quantity, Double subTotal, String imageUrl, Long customerId, String customerName, PaymentMethod paymentMethod, String paymentStatus, OrderStatus orderStatus, LocalDateTime orderDate, Address shippingAddress) {
        this.orderId = orderId;
        this.productId = productId;
        this.merchantId = merchantId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.subTotal = subTotal;
        this.imageUrl = imageUrl;
        this.customerId = customerId;
        this.customerName = customerName;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.orderDate = orderDate;
        this.shippingAddress = shippingAddress;
    }
}
