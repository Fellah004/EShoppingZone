package com.eshoppingzone.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequestDto {

    private Long returnId;
    private Long orderId;
    private Long productId;
    private Long merchantId;
    private Long customerId;
    private String customerName;
    private String productName;
    private Integer quantity;
    private Double refundAmount;
    private String reason;
    private String status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
