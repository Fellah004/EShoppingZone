package com.eshoppingzone.website.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantCreateRequest {

    @NotBlank(message = "Variant name is required (e.g. Size, Color)")
    private String variantName;

    @NotBlank(message = "Variant value is required (e.g. XL, Red)")
    private String variantValue;

    private Double price;

    private Integer stockQuantity;

    private String sku;

    private Boolean active;
}
