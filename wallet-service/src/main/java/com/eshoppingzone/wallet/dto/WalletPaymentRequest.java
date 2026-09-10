package com.eshoppingzone.wallet.dto;

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
public class WalletPaymentRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    private Long orderId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Payment amount must be greater than zero")
    private Double amount;

    private String remarks;
}
