package com.eshoppingzone.order.client;

import com.eshoppingzone.order.dto.AddMoneyRequest;
import com.eshoppingzone.order.dto.TransactionDto;
import com.eshoppingzone.order.dto.WalletPaymentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "wallet-service", url = "${services.wallet-service.url:http://localhost:8085}")
public interface WalletClient {

    @PostMapping("/api/wallet/pay")
    TransactionDto payUsingWallet(@RequestBody WalletPaymentRequest request);

    @PostMapping("/api/wallet/add-money")
    Object addMoney(@RequestBody AddMoneyRequest request);
}
