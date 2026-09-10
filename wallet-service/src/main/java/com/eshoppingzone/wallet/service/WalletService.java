package com.eshoppingzone.wallet.service;

import com.eshoppingzone.wallet.dto.AddMoneyRequest;
import com.eshoppingzone.wallet.dto.TransactionDto;
import com.eshoppingzone.wallet.dto.WalletDto;
import com.eshoppingzone.wallet.dto.WalletPaymentRequest;

import java.util.List;

public interface WalletService {

    WalletDto getOrCreateWallet(Long customerId);

    WalletDto addMoney(AddMoneyRequest request);

    TransactionDto payUsingWallet(WalletPaymentRequest request);

    List<TransactionDto> getTransactionsByCustomerId(Long customerId);

    List<TransactionDto> getAllTransactions();
}
