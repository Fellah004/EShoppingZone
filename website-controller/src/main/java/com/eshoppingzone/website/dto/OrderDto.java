package com.eshoppingzone.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {

    private Long orderId;
    private Long customerId;
    private LocalDateTime orderDate;
    private Double orderAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private AddressDto shippingAddress;
    private Long deliveryAgentId;
    private String deliveryAgentName;
    private LocalDateTime assignedDate;
    private LocalDateTime outForDeliveryDate;
    private LocalDateTime deliveredDate;
    private String refundStatus;
    private String refundReason;
    private String assignmentStatus;
    private String assignmentRejectionReason;
    private String pickupStatus;
    private LocalDateTime pickupDate;
    private String deliveryOtp;
    private String deliveryProofUrl;
    private String deliveryFailureReason;
    private LocalDateTime rescheduledDate;
    private String returnToMerchantStatus;
    @Builder.Default
    private List<OrderItemDto> items = new ArrayList<>();

    public OrderDto(Long orderId, Long customerId, LocalDateTime orderDate, Double orderAmount, String paymentMethod, String paymentStatus, String orderStatus, AddressDto shippingAddress, List<OrderItemDto> items) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.orderAmount = orderAmount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.shippingAddress = shippingAddress;
        this.items = items != null ? items : new ArrayList<>();
    }

    public Double getAmountPaid() {
        return orderAmount;
    }

    public void setAmountPaid(Double amountPaid) {
        this.orderAmount = amountPaid;
    }
}
