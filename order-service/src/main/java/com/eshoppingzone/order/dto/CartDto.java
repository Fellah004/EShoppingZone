package com.eshoppingzone.order.dto;

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
    @Builder.Default
    private Double totalPrice = 0.0;
    @Builder.Default
    private List<CartItemDto> items = new ArrayList<>();
}
