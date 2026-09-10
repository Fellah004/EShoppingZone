package com.eshoppingzone.wallet.service;

import com.eshoppingzone.wallet.dto.AddMoneyRequest;
import com.eshoppingzone.wallet.dto.TransactionDto;
import com.eshoppingzone.wallet.dto.WalletDto;
import com.eshoppingzone.wallet.dto.WalletPaymentRequest;
import com.eshoppingzone.wallet.entity.TransactionType;
import com.eshoppingzone.wallet.entity.Wallet;
import com.eshoppingzone.wallet.entity.WalletTransaction;
import com.eshoppingzone.wallet.exception.InsufficientBalanceException;
import com.eshoppingzone.wallet.repository.WalletRepository;
import com.eshoppingzone.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository, WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public WalletDto getOrCreateWallet(Long customerId) {
        Wallet wallet = findOrCreateWalletEntity(customerId);
        return new WalletDto(wallet.getWalletId(), wallet.getCustomerId(), wallet.getCurrentBalance());
    }

    @Override
    public WalletDto addMoney(AddMoneyRequest request) {
        Wallet wallet = findOrCreateWalletEntity(request.getCustomerId());

        double newBalance = wallet.getCurrentBalance() + request.getAmount();
        wallet.setCurrentBalance(newBalance);
        Wallet saved = walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(saved.getWalletId());
        tx.setCustomerId(saved.getCustomerId());
        tx.setTransactionType(TransactionType.CREDIT);
        tx.setAmount(request.getAmount());
        tx.setTransactionDateTime(LocalDateTime.now());
        tx.setRemarks(request.getRemarks() != null ? request.getRemarks() : "Wallet Recharge");
        tx.setBalanceAfter(newBalance);
        tx.setStatus("SUCCESS");
        transactionRepository.save(tx);

        return new WalletDto(saved.getWalletId(), saved.getCustomerId(), saved.getCurrentBalance());
    }

    @Override
    public TransactionDto payUsingWallet(WalletPaymentRequest request) {
        Wallet wallet = findOrCreateWalletEntity(request.getCustomerId());

        if (wallet.getCurrentBalance() < request.getAmount()) {
            throw new InsufficientBalanceException(String.format(
                    "Insufficient wallet balance. Available balance: ₹%.2f, Required amount: ₹%.2f",
                    wallet.getCurrentBalance(), request.getAmount()
            ));
        }

        double newBalance = wallet.getCurrentBalance() - request.getAmount();
        wallet.setCurrentBalance(newBalance);
        Wallet savedWallet = walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(savedWallet.getWalletId());
        tx.setCustomerId(savedWallet.getCustomerId());
        tx.setTransactionType(TransactionType.DEBIT);
        tx.setAmount(request.getAmount());
        tx.setTransactionDateTime(LocalDateTime.now());
        tx.setOrderId(request.getOrderId());
        tx.setRemarks(request.getOrderId() != null ? "Order #" + request.getOrderId() : (request.getRemarks() != null ? request.getRemarks() : "Order Payment"));
        tx.setBalanceAfter(newBalance);
        tx.setStatus("SUCCESS");

        WalletTransaction savedTx = transactionRepository.save(tx);

        return new TransactionDto(
                savedTx.getTransactionId(),
                savedTx.getWalletId(),
                savedTx.getCustomerId(),
                savedTx.getTransactionType(),
                savedTx.getAmount(),
                savedTx.getTransactionDateTime(),
                savedTx.getOrderId(),
                savedTx.getRemarks(),
                savedTx.getBalanceAfter(),
                savedTx.getStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByCustomerId(Long customerId) {
        return transactionRepository.findByCustomerIdOrderByTransactionDateTimeDesc(customerId).stream()
                .map(tx -> new TransactionDto(
                        tx.getTransactionId(),
                        tx.getWalletId(),
                        tx.getCustomerId(),
                        tx.getTransactionType(),
                        tx.getAmount(),
                        tx.getTransactionDateTime(),
                        tx.getOrderId(),
                        tx.getRemarks(),
                        tx.getBalanceAfter() != null ? tx.getBalanceAfter() : 0.0,
                        tx.getStatus() != null ? tx.getStatus() : "SUCCESS"
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .sorted((a, b) -> b.getTransactionDateTime().compareTo(a.getTransactionDateTime()))
                .map(tx -> new TransactionDto(
                        tx.getTransactionId(),
                        tx.getWalletId(),
                        tx.getCustomerId(),
                        tx.getTransactionType(),
                        tx.getAmount(),
                        tx.getTransactionDateTime(),
                        tx.getOrderId(),
                        tx.getRemarks(),
                        tx.getBalanceAfter() != null ? tx.getBalanceAfter() : 0.0,
                        tx.getStatus() != null ? tx.getStatus() : "SUCCESS"
                ))
                .collect(Collectors.toList());
    }

    private Wallet findOrCreateWalletEntity(Long customerId) {
        return walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setCustomerId(customerId);
                    newWallet.setCurrentBalance(0.0);
                    return walletRepository.save(newWallet);
                });
    }
}
