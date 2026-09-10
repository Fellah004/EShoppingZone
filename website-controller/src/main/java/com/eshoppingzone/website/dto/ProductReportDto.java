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
public class ProductReportDto {
    private Long reportId;
    private String targetType;
    private Long targetId;
    private Long customerId;
    private String reason;
    private String comments;
    private LocalDateTime reportDate;
    private String status;
}
