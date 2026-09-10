package com.eshoppingzone.wallet.listener;

import com.eshoppingzone.wallet.config.RabbitMQConfig;
import com.eshoppingzone.wallet.dto.OrderPlacedEvent;
import com.eshoppingzone.wallet.entity.TransactionType;
import com.eshoppingzone.wallet.entity.WalletTransaction;
import com.eshoppingzone.wallet.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final WalletTransactionRepository transactionRepository;

    public OrderEventListener(WalletTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.WALLET_QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent via RabbitMQ for Order ID: {}, Customer ID: {}, Amount: {}, PaymentMethod: {}",
                event.getOrderId(), event.getCustomerId(), event.getAmount(), event.getPaymentMethod());

        if ("WALLET".equalsIgnoreCase(event.getPaymentMethod())) {
            // Find recent debit transaction for this customer that doesn't have an orderId set and attach it
            List<WalletTransaction> customerTxns = transactionRepository.findByCustomerIdOrderByTransactionDateTimeDesc(event.getCustomerId());
            for (WalletTransaction tx : customerTxns) {
                if (tx.getTransactionType() == TransactionType.DEBIT && tx.getOrderId() == null) {
                    tx.setOrderId(event.getOrderId());
                    tx.setRemarks("Payment for Order #" + event.getOrderId());
                    transactionRepository.save(tx);
                    log.info("Updated Wallet Transaction ID: {} with Order ID: #{}", tx.getTransactionId(), event.getOrderId());
                    break;
                }
            }
        }
    }
}
