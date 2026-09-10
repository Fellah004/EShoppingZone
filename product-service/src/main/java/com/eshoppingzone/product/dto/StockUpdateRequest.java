package com.eshoppingzone.product.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUpdateRequest {

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    private String operation; // "SET" (default), "ADD", "RESTOCK"
}
