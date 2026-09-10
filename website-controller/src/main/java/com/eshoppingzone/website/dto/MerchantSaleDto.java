package com.eshoppingzone.website.dto;

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
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private LocalDateTime orderDate;
    private AddressDto shippingAddress;
    private Long deliveryAgentId;
    private String deliveryAgentName;

    public MerchantSaleDto(Long orderId, Long productId, Long merchantId, String productName, Double price, Integer quantity, Double subTotal, String imageUrl, Long customerId, String customerName, String paymentMethod, String paymentStatus, String orderStatus, LocalDateTime orderDate, AddressDto shippingAddress) {
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

    public Double getTotalAmount() {
        if (subTotal != null) {
            return subTotal;
        }
        if (price != null && quantity != null) {
            return price * quantity;
        }
        return 0.0;
    }
}

