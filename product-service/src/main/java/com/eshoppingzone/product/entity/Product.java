package com.eshoppingzone.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    private Long merchantId;

    @Column(nullable = false)
    private String productType;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String manufacturer;

    @Column(length = 1000)
    private String image;

    @Column(nullable = false)
    private Double price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String specification;

    private Double rating;

    private String review;

    @Column(nullable = false)
    @Builder.Default
    private Integer stockQuantity = 100;

    @Column(nullable = false)
    @Builder.Default
    private Boolean inStock = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Double discountPercent = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @Column(nullable = false)
    @Builder.Default
    private String approvalStatus = "APPROVED";

    private String rejectionReason;

    private String brand;

    private String subcategory;

    public Double getDiscountedPrice() {
        if (price == null) return 0.0;
        if (discountPercent != null && discountPercent > 0.0) {
            double effective = price * (1.0 - (discountPercent / 100.0));
            return Math.round(effective * 100.0) / 100.0;
        }
        return price;
    }

    public boolean isLowStock() {
        int threshold = (lowStockThreshold != null) ? lowStockThreshold : 10;
        return stockQuantity != null && stockQuantity <= threshold;
    }

    public Product(Long productId, Long merchantId, String productType, String productName, String category, String manufacturer, String image, Double price, String description, String specification) {
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
        this.lowStockThreshold = 10;
        this.approvalStatus = "APPROVED";
    }

    public Product(Long productId, Long merchantId, String productType, String productName, String category, String manufacturer, String image, Double price, String description, String specification, Double rating, String review) {
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
        this.stockQuantity = 100;
        this.inStock = true;
        this.active = true;
        this.discountPercent = 0.0;
        this.lowStockThreshold = 10;
        this.approvalStatus = "APPROVED";
    }

    @PrePersist
    public void prePersist() {
        if (this.approvalStatus == null) {
            this.approvalStatus = "APPROVED";
        }
        if (this.active == null) {
            this.active = true;
        }
        if (this.inStock == null) {
            this.inStock = true;
        }
        if (this.discountPercent == null) {
            this.discountPercent = 0.0;
        }
        if (this.lowStockThreshold == null) {
            this.lowStockThreshold = 10;
        }
    }
}
