package com.eshoppingzone.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantDto {

    private Long variantId;
    private Long productId;
    private String variantName;
    private String variantValue;
    private Double price;
    private Integer stockQuantity;
    private String sku;
    private Boolean active;
}
