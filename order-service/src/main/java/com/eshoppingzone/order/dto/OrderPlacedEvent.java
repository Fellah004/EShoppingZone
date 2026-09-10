package com.eshoppingzone.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPlacedEvent implements Serializable {

    private Long orderId;
    private Long customerId;
    private Double amount;
    private String paymentMethod;
    private LocalDateTime orderDate;
}
