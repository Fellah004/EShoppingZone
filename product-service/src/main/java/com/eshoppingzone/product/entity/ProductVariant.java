package com.eshoppingzone.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_variants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long variantId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String variantName;

    @Column(nullable = false)
    private String variantValue;

    private Double price;

    @Column(nullable = false)
    @Builder.Default
    private Integer stockQuantity = 10;

    private String sku;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
