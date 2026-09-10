package com.eshoppingzone.website.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateRequest {

    private Long merchantId;

    @NotBlank(message = "Product name is required")
    private String productName;

    @NotBlank(message = "Product type is required")
    private String productType;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Manufacturer is required")
    private String manufacturer;

    private String image;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private Double price;

    private String description;

    private String specification;

    private Integer stockQuantity;

    private Boolean inStock;

    private Boolean active;

    private Double discountPercent;

    private Integer lowStockThreshold;

    public ProductUpdateRequest(String productName, String productType, String category, String manufacturer, String image, Double price, String description, String specification) {
        this.productName = productName;
        this.productType = productType;
        this.category = category;
        this.manufacturer = manufacturer;
        this.image = image;
        this.price = price;
        this.description = description;
        this.specification = specification;
    }
}
