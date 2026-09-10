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
public class CustomerComplaintDto {

    private Long complaintId;
    private Long orderId;
    private Long customerId;
    private String customerName;
    private Long merchantId;
    private String subject;
    private String description;
    private String complaintType;
    private String status;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
