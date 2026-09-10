package com.eshoppingzone.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {

    private Long transactionId;
    private Long walletId;
    private Long customerId;
    private String transactionType;
    private Double amount;
    private LocalDateTime transactionDateTime;
    private Long orderId;
    private String remarks;
}
