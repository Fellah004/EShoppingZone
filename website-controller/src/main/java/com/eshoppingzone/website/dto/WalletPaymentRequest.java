package com.eshoppingzone.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletPaymentRequest {

    private Long customerId;
    private Long orderId;
    private Double amount;
    private String remarks;
}
