package com.eshoppingzone.profile.listener;

import com.eshoppingzone.profile.config.RabbitMQConfig;
import com.eshoppingzone.profile.dto.*;
import com.eshoppingzone.profile.entity.Role;
import com.eshoppingzone.profile.entity.UserProfile;
import com.eshoppingzone.profile.repository.UserProfileRepository;
import com.eshoppingzone.profile.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    public OrderEventListener(
            NotificationService notificationService,
            UserProfileRepository userProfileRepository,
            ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.userProfileRepository = userProfileRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotificationEvents(Message message) {
        try {
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("Received RabbitMQ message with routing key [{}]: {}", routingKey, json);

            if (RabbitMQConfig.ORDER_PLACED_ROUTING_KEY.equals(routingKey) || json.contains("paymentMethod")) {
                OrderPlacedEvent placedEvent = objectMapper.readValue(json, OrderPlacedEvent.class);
                handleOrderPlaced(placedEvent);
            } else if (RabbitMQConfig.ORDER_ASSIGNED_ROUTING_KEY.equals(routingKey) || (json.contains("deliveryAgentId") && !json.contains("previousStatus"))) {
                OrderAssignedEvent assignedEvent = objectMapper.readValue(json, OrderAssignedEvent.class);
                handleOrderAssigned(assignedEvent);
            } else if (RabbitMQConfig.ORDER_STATUS_UPDATED_ROUTING_KEY.equals(routingKey) || json.contains("newStatus")) {
                OrderStatusUpdatedEvent statusEvent = objectMapper.readValue(json, OrderStatusUpdatedEvent.class);
                handleOrderStatusUpdated(statusEvent);
            } else {
                log.warn("Unrecognized notification message format: {}", json);
            }
        } catch (Exception ex) {
            log.error("Failed to deserialize and process notification event: {}", ex.getMessage(), ex);
        }
    }

    private void notifyAdmins(String title, String message, String type) {
        try {
            List<UserProfile> admins = userProfileRepository.findByRole(Role.ADMIN);
            for (UserProfile admin : admins) {
                notificationService.createNotification(new CreateNotificationRequest(
                        admin.getProfileId(),
                        "ADMIN",
                        title,
                        message,
                        type
                ));
            }
        } catch (Exception ex) {
            log.warn("Failed to notify admins", ex);
        }
    }

    private void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Processing OrderPlacedEvent for Customer: {}, Order: #{}", event.getCustomerId(), event.getOrderId());

        // 1. Customer Notification: Order Placed
        notificationService.createNotification(new CreateNotificationRequest(
                event.getCustomerId(),
                "CUSTOMER",
                "Order Placed",
                String.format("Your order #%d has been placed successfully for ₹%.2f.", event.getOrderId(), event.getAmount()),
                "ORDER"
        ));

        // 2. Customer Notification: Payment Successful (for wallet) or COD
        if ("WALLET".equalsIgnoreCase(event.getPaymentMethod())) {
            notificationService.createNotification(new CreateNotificationRequest(
                    event.getCustomerId(),
                    "CUSTOMER",
                    "Payment Successful",
                    String.format("Payment of ₹%.2f was completed successfully for Order #%d.", event.getAmount(), event.getOrderId()),
                    "WALLET"
            ));
        } else if ("COD".equalsIgnoreCase(event.getPaymentMethod())) {
            notificationService.createNotification(new CreateNotificationRequest(
                    event.getCustomerId(),
                    "CUSTOMER",
                    "Cash on Delivery",
                    String.format("Order #%d confirmed with Cash on Delivery (₹%.2f to be collected upon delivery).", event.getOrderId(), event.getAmount()),
                    "ORDER"
            ));
        }

        // 3. Admin Notification: New Order Placed
        notifyAdmins(
                "New Order Received",
                String.format("Order #%d was placed by Customer #%d for ₹%.2f. Ready for delivery agent assignment.", event.getOrderId(), event.getCustomerId(), event.getAmount()),
                "ORDER"
        );
    }

    private void handleOrderAssigned(OrderAssignedEvent event) {
        log.info("Processing OrderAssignedEvent for Order: #{}, Agent: {}", event.getOrderId(), event.getDeliveryAgentName());

        // 1. Delivery Agent Notification
        if (event.getDeliveryAgentId() != null) {
            notificationService.createNotification(new CreateNotificationRequest(
                    event.getDeliveryAgentId(),
                    "DELIVERY_AGENT",
                    "New Assignment",
                    String.format("Order #%d (₹%.2f) has been assigned to you for delivery.", event.getOrderId(), event.getOrderAmount()),
                    "DELIVERY"
            ));
        }

        // 2. Customer Notification
        notificationService.createNotification(new CreateNotificationRequest(
                event.getCustomerId(),
                "CUSTOMER",
                "Order Assigned",
                String.format("Your order #%d has been assigned to Delivery Agent %s.", event.getOrderId(), event.getDeliveryAgentName()),
                "ORDER"
        ));

        // 3. Merchant Notifications
        if (event.getMerchantIds() != null) {
            for (Long merchantId : event.getMerchantIds()) {
                notificationService.createNotification(new CreateNotificationRequest(
                        merchantId,
                        "MERCHANT",
                        "Order Assigned",
                        String.format("Order #%d containing your product(s) has been assigned to Delivery Agent %s.", event.getOrderId(), event.getDeliveryAgentName()),
                        "SALE"
                ));
            }
        }

        // 4. Admin Notification
        notifyAdmins(
                "Order Dispatched",
                String.format("Order #%d has been assigned to Delivery Agent %s.", event.getOrderId(), event.getDeliveryAgentName()),
                "DELIVERY"
        );
    }

    private void handleOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        log.info("Processing OrderStatusUpdatedEvent for Order: #{}, Status: {}", event.getOrderId(), event.getNewStatus());

        String newStatus = event.getNewStatus();

        if ("OUT_FOR_DELIVERY".equalsIgnoreCase(newStatus)) {
            // Customer notification
            notificationService.createNotification(new CreateNotificationRequest(
                    event.getCustomerId(),
                    "CUSTOMER",
                    "Order Status Updated",
                    String.format("Your order #%d is now Out for Delivery.", event.getOrderId()),
                    "DELIVERY"
            ));

            // Merchant notification
            if (event.getMerchantIds() != null) {
                for (Long merchantId : event.getMerchantIds()) {
                    notificationService.createNotification(new CreateNotificationRequest(
                            merchantId,
                            "MERCHANT",
                            "Delivery Status Updated",
                            String.format("Order #%d is now Out for Delivery.", event.getOrderId()),
                            "SALE"
                    ));
                }
            }

            // Admin notification
            notifyAdmins(
                    "Order Out for Delivery",
                    String.format("Order #%d is currently Out for Delivery.", event.getOrderId()),
                    "DELIVERY"
            );
        } else if ("DELIVERED".equalsIgnoreCase(newStatus)) {
            // Customer notification
            notificationService.createNotification(new CreateNotificationRequest(
                    event.getCustomerId(),
                    "CUSTOMER",
                    "Order Delivered",
                    String.format("Your order #%d has been delivered successfully.", event.getOrderId()),
                    "DELIVERY"
            ));

            // Merchant notification
            if (event.getMerchantIds() != null) {
                for (Long merchantId : event.getMerchantIds()) {
                    notificationService.createNotification(new CreateNotificationRequest(
                            merchantId,
                            "MERCHANT",
                            "Order Delivered",
                            String.format("Order #%d has been delivered successfully.", event.getOrderId()),
                            "SALE"
                    ));
                }
            }

            // Admin notification
            notifyAdmins(
                    "Order Completed",
                    String.format("Order #%d has been successfully Delivered.", event.getOrderId()),
                    "DELIVERY"
            );
        }
    }
}
