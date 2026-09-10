package com.eshoppingzone.wallet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(nullable = false)
    private Long walletId;

    @Column(nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private LocalDateTime transactionDateTime;

    private Long orderId;

    private String remarks;

    private Double balanceAfter;

    private String status;

    public WalletTransaction(Long transactionId, Long walletId, Long customerId, TransactionType transactionType, Double amount, LocalDateTime transactionDateTime, Long orderId, String remarks) {
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
