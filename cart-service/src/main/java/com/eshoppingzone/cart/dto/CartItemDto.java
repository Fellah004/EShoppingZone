package com.eshoppingzone.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDto {

    private Long cartItemId;
    private Long productId;
    private Long merchantId;
    private String productName;
    private Double price;
    private Integer quantity;
    private Double subTotal;
    private String imageUrl;
}
