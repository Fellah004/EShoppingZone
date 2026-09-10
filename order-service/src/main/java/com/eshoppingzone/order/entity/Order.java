package com.eshoppingzone.order.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private Double orderAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, length = 50)
    private String paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus orderStatus;

    @Embedded
    private Address shippingAddress;

    private Long deliveryAgentId;

    private String deliveryAgentName;

    private LocalDateTime assignedDate;

    private LocalDateTime outForDeliveryDate;

    private LocalDateTime deliveredDate;

    @Column(nullable = false)
    @Builder.Default
    private String refundStatus = "NONE";

    private String refundReason;

    @Column(nullable = false)
    @Builder.Default
    private String assignmentStatus = "NONE";

    private String assignmentRejectionReason;

    @Column(nullable = false)
    @Builder.Default
    private String pickupStatus = "PENDING";

    private LocalDateTime pickupDate;

    private String deliveryOtp;

    private String deliveryProofUrl;

    private String deliveryFailureReason;

    private LocalDateTime rescheduledDate;

    @Column(nullable = false)
    @Builder.Default
    private String returnToMerchantStatus = "NONE";

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public Order(Long orderId, Long customerId, LocalDateTime orderDate, Double orderAmount, PaymentMethod paymentMethod, String paymentStatus, OrderStatus orderStatus, Address shippingAddress, List<OrderItem> items) {
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
}
