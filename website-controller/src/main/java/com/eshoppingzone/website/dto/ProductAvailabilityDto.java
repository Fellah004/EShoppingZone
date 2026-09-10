package com.eshoppingzone.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAvailabilityDto {
    private Long productId;
    private String productName;
    private Boolean inStock;
    private Integer stockQuantity;
    private String message;
}
