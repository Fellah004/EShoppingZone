package com.eshoppingzone.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {

    private Long orderItemId;
    private Long productId;
    private Long merchantId;
    private String productName;
    private Double price;
    private Integer quantity;
    private Double subTotal;
    private String imageUrl;

    public OrderItemDto(Long orderItemId, Long productId, String productName, Double price, Integer quantity, Double subTotal, String imageUrl) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.subTotal = subTotal;
        this.imageUrl = imageUrl;
    }
}
