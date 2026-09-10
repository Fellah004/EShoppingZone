package com.eshoppingzone.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {

    private Long productId;
    private Long merchantId;
    private String productType;
    private String productName;
    private String category;
    private String manufacturer;
    private String image;
    private Double price;
    private String description;
    private String specification;
    private Double discountPercent;
    private Double discountedPrice;
    private Boolean inStock;
    private Integer stockQuantity;

    public Double getEffectivePrice() {
        if (discountedPrice != null && discountedPrice > 0) {
            return discountedPrice;
        }
        if (price != null && discountPercent != null && discountPercent > 0) {
            return Math.round(price * (1.0 - discountPercent / 100.0) * 100.0) / 100.0;
        }
        return price != null ? price : 0.0;
    }
}
