package com.eshoppingzone.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {

    @NotBlank(message = "Target type is required (PRODUCT or REVIEW)")
    private String targetType;

    @NotNull(message = "Target ID is required")
    private Long targetId;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String comments;
}
