package com.eshoppingzone.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDto {

    private Long cartId;
    private Long customerId;
    private Double totalPrice;
    @Builder.Default
    private List<CartItemDto> items = new ArrayList<>();
}
