package com.eshoppingzone.website.dto;

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
    private Double balanceAfter;
    private String status;

    public TransactionDto(Long transactionId, Long walletId, Long customerId, String transactionType, Double amount, LocalDateTime transactionDateTime, Long orderId, String remarks) {
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.customerId = customerId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDateTime = transactionDateTime;
        this.orderId = orderId;
        this.remarks = remarks;
        this.status = "SUCCESS";
    }
}
