package com.eshoppingzone.product.dto;

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
    private Double rating;
    private String review;
    private Integer stockQuantity;
    private Boolean inStock;
    private Boolean active;
    private Double discountPercent;
    private Double discountedPrice;
    private Integer lowStockThreshold;
    private String approvalStatus;
    private String rejectionReason;
    private String brand;
    private String subcategory;
    private java.util.List<ProductVariantDto> variants;

    public ProductDto(Long productId, Long merchantId, String productType, String productName, String category, String manufacturer, String image, Double price, String description, String specification) {
        this.productId = productId;
        this.merchantId = merchantId;
        this.productType = productType;
        this.productName = productName;
        this.category = category;
        this.manufacturer = manufacturer;
        this.image = image;
        this.price = price;
        this.description = description;
        this.specification = specification;
        this.stockQuantity = 100;
        this.inStock = true;
        this.active = true;
        this.discountPercent = 0.0;
        this.discountedPrice = price;
        this.lowStockThreshold = 10;
        this.variants = new java.util.ArrayList<>();
    }

    public ProductDto(Long productId, Long merchantId, String productType, String productName, String category, String manufacturer, String image, Double price, String description, String specification, Double rating, String review, Integer stockQuantity, Boolean inStock, Boolean active, Double discountPercent, Double discountedPrice, Integer lowStockThreshold) {
        this.productId = productId;
        this.merchantId = merchantId;
        this.productType = productType;
        this.productName = productName;
        this.category = category;
        this.manufacturer = manufacturer;
        this.image = image;
        this.price = price;
        this.description = description;
        this.specification = specification;
        this.rating = rating;
        this.review = review;
        this.stockQuantity = stockQuantity != null ? stockQuantity : 100;
        this.inStock = inStock != null ? inStock : true;
        this.active = active != null ? active : true;
        this.discountPercent = discountPercent != null ? discountPercent : 0.0;
        this.discountedPrice = discountedPrice != null ? discountedPrice : (this.price != null ? this.price : 0.0);
        this.lowStockThreshold = lowStockThreshold != null ? lowStockThreshold : 10;
        this.variants = new java.util.ArrayList<>();
    }

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
