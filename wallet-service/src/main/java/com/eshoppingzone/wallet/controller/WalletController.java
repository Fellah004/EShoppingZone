package com.eshoppingzone.wallet.controller;

import com.eshoppingzone.wallet.dto.AddMoneyRequest;
import com.eshoppingzone.wallet.dto.TransactionDto;
import com.eshoppingzone.wallet.dto.WalletDto;
import com.eshoppingzone.wallet.dto.WalletPaymentRequest;
import com.eshoppingzone.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<WalletDto> getWallet(@PathVariable("customerId") Long customerId) {
        return ResponseEntity.ok(walletService.getOrCreateWallet(customerId));
    }

    @PostMapping("/add-money")
    public ResponseEntity<WalletDto> addMoney(@Valid @RequestBody AddMoneyRequest request) {
        return ResponseEntity.ok(walletService.addMoney(request));
    }

    @PostMapping("/pay")
    public ResponseEntity<TransactionDto> payUsingWallet(@Valid @RequestBody WalletPaymentRequest request) {
        return ResponseEntity.ok(walletService.payUsingWallet(request));
    }

    @GetMapping({"/transactions/all", "/transactions"})
    public ResponseEntity<List<TransactionDto>> getAllTransactions() {
        return ResponseEntity.ok(walletService.getAllTransactions());
    }

    @GetMapping({"/{customerId:\\d+}/transactions", "/transactions/{customerId:\\d+}"})
    public ResponseEntity<List<TransactionDto>> getTransactions(@PathVariable("customerId") Long customerId) {
        return ResponseEntity.ok(walletService.getTransactionsByCustomerId(customerId));
    }
}
