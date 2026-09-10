package com.eshoppingzone.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReviewDto {
    private Long reviewId;
    private Long productId;
    private Long customerId;
    private String customerName;
    private Double rating;
    private String comment;
    private LocalDateTime createdAt;
}
