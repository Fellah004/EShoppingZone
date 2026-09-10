package com.eshoppingzone.website.client;

import com.eshoppingzone.website.dto.AddMoneyRequest;
import com.eshoppingzone.website.dto.TransactionDto;
import com.eshoppingzone.website.dto.WalletDto;
import com.eshoppingzone.website.dto.WalletPaymentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "wallet-service", url = "${services.wallet-service.url:http://localhost:8085}")
public interface WalletClient {

    @GetMapping("/api/wallet/{customerId}")
    WalletDto getWallet(@PathVariable("customerId") Long customerId);

    @PostMapping("/api/wallet/add-money")
    WalletDto addMoney(@RequestBody AddMoneyRequest request);

    @PostMapping("/api/wallet/pay")
    TransactionDto payUsingWallet(@RequestBody WalletPaymentRequest request);

    @GetMapping("/api/wallet/{customerId}/transactions")
    List<TransactionDto> getTransactions(@PathVariable("customerId") Long customerId);

    @GetMapping("/api/wallet/transactions/all")
    List<TransactionDto> getAllTransactions();
}
