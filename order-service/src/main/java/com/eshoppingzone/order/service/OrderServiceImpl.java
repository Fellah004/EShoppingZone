package com.eshoppingzone.order.service;

import com.eshoppingzone.order.client.CartClient;
import com.eshoppingzone.order.client.WalletClient;
import com.eshoppingzone.order.config.RabbitMQConfig;
import com.eshoppingzone.order.dto.*;
import com.eshoppingzone.order.entity.*;
import com.eshoppingzone.order.exception.InvalidRequestException;
import com.eshoppingzone.order.exception.ResourceNotFoundException;
import com.eshoppingzone.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final WalletClient walletClient;
    private final com.eshoppingzone.order.client.ProfileClient profileClient;
    private final com.eshoppingzone.order.client.NotificationClient notificationClient;
    private final com.eshoppingzone.order.client.ProductClient productClient;
    private final com.eshoppingzone.order.repository.ReturnRequestRepository returnRequestRepository;
    private final com.eshoppingzone.order.repository.CustomerComplaintRepository complaintRepository;
    private final RabbitTemplate rabbitTemplate;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            CartClient cartClient,
            WalletClient walletClient,
            com.eshoppingzone.order.client.ProfileClient profileClient,
            com.eshoppingzone.order.client.NotificationClient notificationClient,
            com.eshoppingzone.order.client.ProductClient productClient,
            com.eshoppingzone.order.repository.ReturnRequestRepository returnRequestRepository,
            com.eshoppingzone.order.repository.CustomerComplaintRepository complaintRepository,
            RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.cartClient = cartClient;
        this.walletClient = walletClient;
        this.profileClient = profileClient;
        this.notificationClient = notificationClient;
        this.productClient = productClient;
        this.returnRequestRepository = returnRequestRepository;
        this.complaintRepository = complaintRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public OrderDto createOrder(CreateOrderRequest request) {
        // 1. Fetch Cart
        CartDto cart = cartClient.getCartByCustomerId(request.getCustomerId());
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new InvalidRequestException("Cannot place order with an empty cart");
        }

        double totalAmount = cart.getTotalPrice();

        // 2. Create and Save initial Order Entity
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setOrderDate(LocalDateTime.now());
        order.setOrderAmount(totalAmount);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(request.getPaymentMethod() == PaymentMethod.WALLET || request.getPaymentMethod() == PaymentMethod.ONLINE ? "PAID" : "PENDING");
        order.setOrderStatus(OrderStatus.PLACED);
        order.setShippingAddress(request.getShippingAddress());

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemDto cartItem : cart.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductId(cartItem.getProductId());
            item.setMerchantId(cartItem.getMerchantId());
            item.setProductName(cartItem.getProductName());
            item.setPrice(cartItem.getPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setSubTotal(cartItem.getSubTotal());
            item.setImageUrl(cartItem.getImageUrl());
            item.setOrder(order);
            orderItems.add(item);
        }
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // 3. Handle Wallet Payment with the saved Order ID
        if (request.getPaymentMethod() == PaymentMethod.WALLET) {
            WalletPaymentRequest walletPaymentRequest = new WalletPaymentRequest(
                    request.getCustomerId(),
                    savedOrder.getOrderId(),
                    totalAmount,
                    "Payment for Order #" + savedOrder.getOrderId()
            );
            // If wallet balance is insufficient, this throws an exception and the transaction rolls back
            walletClient.payUsingWallet(walletPaymentRequest);
        }

        // Deduct inventory in Product Service
        for (OrderItem item : savedOrder.getItems()) {
            if (item.getProductId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                try {
                    productClient.deductStock(item.getProductId(), item.getQuantity());
                } catch (Exception ex) {
                    log.warn("Failed to deduct stock for product #{}: {}", item.getProductId(), ex.getMessage());
                }
            }
        }

        // 4. Clear Cart
        try {
            cartClient.clearCart(request.getCustomerId());
        } catch (Exception ex) {
            log.warn("Failed to clear cart after order creation for customer: {}", request.getCustomerId(), ex);
        }

        // 5. Send In-App Notifications (Direct & Resilient)
        sendNotification(savedOrder.getCustomerId(), "CUSTOMER", "Order Placed Successfully!", "Your order #" + savedOrder.getOrderId() + " for ₹" + String.format("%.2f", savedOrder.getOrderAmount()) + " has been placed successfully.", "ORDER_PLACED");
        for (OrderItem item : savedOrder.getItems()) {
            if (item.getMerchantId() != null) {
                sendNotification(item.getMerchantId(), "MERCHANT", "New Order Received!", "Order #" + savedOrder.getOrderId() + " containing " + item.getProductName() + " (Qty: " + item.getQuantity() + ") has been placed.", "ORDER_PLACED");
            }
        }
        notifyAdmins("New Order Received", "Order #" + savedOrder.getOrderId() + " placed by customer #" + savedOrder.getCustomerId() + " for ₹" + String.format("%.2f", savedOrder.getOrderAmount()) + ".", "ORDER_PLACED");

        // 6. Publish RabbitMQ OrderPlacedEvent
        try {
            OrderPlacedEvent event = new OrderPlacedEvent(
                    savedOrder.getOrderId(),
                    savedOrder.getCustomerId(),
                    savedOrder.getOrderAmount(),
                    savedOrder.getPaymentMethod().name(),
                    savedOrder.getOrderDate()
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_PLACED_ROUTING_KEY,
                    event
            );
            log.info("Published OrderPlacedEvent for Order ID: {}", savedOrder.getOrderId());
        } catch (Exception ex) {
            log.warn("Failed to publish OrderPlacedEvent to RabbitMQ for Order ID: {}", savedOrder.getOrderId(), ex);
        }

        return mapToDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return mapToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByDeliveryAgentId(Long deliveryAgentId) {
        return orderRepository.findByDeliveryAgentIdOrderByOrderDateDesc(deliveryAgentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchantSaleDto> getMerchantSales(Long merchantId) {
        List<Order> orders;
        if (merchantId == null) {
            orders = orderRepository.findAllByOrderByOrderDateDesc();
        } else {
            orders = orderRepository.findOrdersByMerchantId(merchantId);
        }

        List<MerchantSaleDto> sales = new ArrayList<>();
        for (Order order : orders) {
            String customerName = order.getShippingAddress() != null && order.getShippingAddress().getFullName() != null
                    ? order.getShippingAddress().getFullName()
                    : "Customer #" + order.getCustomerId();

            for (OrderItem item : order.getItems()) {
                if (merchantId == null || item.getMerchantId() == null || item.getMerchantId().equals(merchantId)) {
                    sales.add(new MerchantSaleDto(
                            order.getOrderId(),
                            item.getProductId(),
                            item.getMerchantId(),
                            item.getProductName(),
                            item.getPrice(),
                            item.getQuantity(),
                            item.getSubTotal(),
                            item.getImageUrl(),
                            order.getCustomerId(),
                            customerName,
                            order.getPaymentMethod(),
                            order.getPaymentStatus(),
                            order.getOrderStatus(),
                            order.getOrderDate(),
                            order.getShippingAddress(),
                            order.getDeliveryAgentId(),
                            order.getDeliveryAgentName() != null ? order.getDeliveryAgentName() : "Unassigned"
                    ));
                }
            }
        }
        return sales;
    }

    @Override
    public OrderDto updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setOrderStatus(request.getOrderStatus());
        Order updated = orderRepository.save(order);
        return mapToDto(updated);
    }

    @Override
    public OrderDto assignDeliveryAgent(Long orderId, AssignDeliveryAgentRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setDeliveryAgentId(request.getDeliveryAgentId());
        String agentName = request.getDeliveryAgentName();
        if (agentName == null || agentName.trim().isEmpty()) {
            try {
                UserProfileDto agentProfile = profileClient.getProfile(request.getDeliveryAgentId());
                if (agentProfile != null && agentProfile.getFullName() != null) {
                    agentName = agentProfile.getFullName();
                }
            } catch (Exception ignored) {
            }
        }
        if (agentName == null || agentName.trim().isEmpty()) {
            agentName = "Delivery Agent #" + request.getDeliveryAgentId();
        }
        order.setDeliveryAgentName(agentName);
        order.setOrderStatus(OrderStatus.ASSIGNED);
        order.setAssignmentStatus("ASSIGNED");
        order.setPickupStatus("PENDING");
        order.setAssignmentRejectionReason(null);
        order.setAssignedDate(LocalDateTime.now());

        Order saved = orderRepository.save(order);

        // Publish OrderAssignedEvent
        try {
            List<Long> merchantIds = saved.getItems().stream()
                    .map(OrderItem::getMerchantId)
                    .filter(m -> m != null)
                    .distinct()
                    .collect(Collectors.toList());

            OrderAssignedEvent event = new OrderAssignedEvent(
                    saved.getOrderId(),
                    saved.getCustomerId(),
                    saved.getDeliveryAgentId(),
                    saved.getDeliveryAgentName(),
                    saved.getOrderAmount(),
                    saved.getOrderStatus().name(),
                    merchantIds,
                    saved.getAssignedDate()
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_ASSIGNED_ROUTING_KEY,
                    event
            );
            log.info("Published OrderAssignedEvent for Order ID: {} to Agent: {}", saved.getOrderId(), saved.getDeliveryAgentName());
        } catch (Exception ex) {
            log.warn("Failed to publish OrderAssignedEvent to RabbitMQ: {}", ex.getMessage());
        }

        // Direct In-App Notifications
        sendNotification(saved.getCustomerId(), "CUSTOMER", "Delivery Agent Assigned", "Your order #" + saved.getOrderId() + " has been assigned to delivery specialist " + saved.getDeliveryAgentName() + ".", "ORDER_ASSIGNED");
        sendNotification(saved.getDeliveryAgentId(), "DELIVERY_AGENT", "New Delivery Assignment", "You have been assigned to deliver Order #" + saved.getOrderId() + " to " + (saved.getShippingAddress() != null ? saved.getShippingAddress().getCity() : "Customer") + ".", "ORDER_ASSIGNED");
        notifyAdmins("Order Assigned", "Order #" + saved.getOrderId() + " was assigned to delivery agent " + saved.getDeliveryAgentName() + ".", "ORDER_ASSIGNED");

        return mapToDto(saved);
    }

    @Override
    public OrderDto updateDeliveryStatus(Long orderId, UpdateDeliveryStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Validate agent ownership
        if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(request.getDeliveryAgentId())) {
            throw new InvalidRequestException("Access denied: You are not assigned to deliver Order #" + orderId);
        }

        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus newStatus = request.getNewStatus();

        // Validate Status Transition Progression:
        // ASSIGNED -> OUT_FOR_DELIVERY -> DELIVERED
        if (currentStatus == OrderStatus.ASSIGNED) {
            if (newStatus != OrderStatus.OUT_FOR_DELIVERY && newStatus != OrderStatus.CANCELLED) {
                throw new InvalidRequestException("Invalid transition: Order in ASSIGNED state must move to OUT_FOR_DELIVERY before DELIVERED.");
            }
        } else if (currentStatus == OrderStatus.OUT_FOR_DELIVERY) {
            if (newStatus != OrderStatus.DELIVERED && newStatus != OrderStatus.CANCELLED) {
                throw new InvalidRequestException("Invalid transition: Order in OUT_FOR_DELIVERY state can only transition to DELIVERED or CANCELLED.");
            }
        } else if (currentStatus == OrderStatus.DELIVERED) {
            throw new InvalidRequestException("Order #" + orderId + " is already DELIVERED and cannot be modified.");
        } else {
            throw new InvalidRequestException("Invalid transition from " + currentStatus + " to " + newStatus);
        }

        String previousStatusStr = currentStatus.name();
        order.setOrderStatus(newStatus);

        if (newStatus == OrderStatus.OUT_FOR_DELIVERY) {
            order.setOutForDeliveryDate(LocalDateTime.now());
        } else if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredDate(LocalDateTime.now());
        }

        Order saved = orderRepository.save(order);

        // Publish OrderStatusUpdatedEvent
        try {
            List<Long> merchantIds = saved.getItems().stream()
                    .map(OrderItem::getMerchantId)
                    .filter(m -> m != null)
                    .distinct()
                    .collect(Collectors.toList());

            OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
                    saved.getOrderId(),
                    saved.getCustomerId(),
                    saved.getDeliveryAgentId(),
                    saved.getDeliveryAgentName(),
                    previousStatusStr,
                    saved.getOrderStatus().name(),
                    saved.getOrderAmount(),
                    merchantIds,
                    LocalDateTime.now()
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_STATUS_UPDATED_ROUTING_KEY,
                    event
            );
            log.info("Published OrderStatusUpdatedEvent for Order ID: {} ({})", saved.getOrderId(), newStatus);
        } catch (Exception ex) {
            log.warn("Failed to publish OrderStatusUpdatedEvent to RabbitMQ: {}", ex.getMessage());
        }

        // Direct In-App Notifications
        String statusLabel = newStatus == OrderStatus.OUT_FOR_DELIVERY ? "Out for Delivery" : (newStatus == OrderStatus.DELIVERED ? "Delivered" : newStatus.name());
        sendNotification(saved.getCustomerId(), "CUSTOMER", "Order " + statusLabel, "Order #" + saved.getOrderId() + " is now " + statusLabel + ". Handled by " + saved.getDeliveryAgentName() + ".", "STATUS_UPDATED");
        for (OrderItem item : saved.getItems()) {
            if (item.getMerchantId() != null) {
                sendNotification(item.getMerchantId(), "MERCHANT", "Order Status Updated", "Order #" + saved.getOrderId() + " status is now " + statusLabel + ".", "STATUS_UPDATED");
            }
        }
        sendNotification(saved.getDeliveryAgentId(), "DELIVERY_AGENT", "Status Updated", "You marked Order #" + saved.getOrderId() + " as " + statusLabel + ".", "STATUS_UPDATED");
        notifyAdmins("Order Status Updated", "Order #" + saved.getOrderId() + " marked as " + statusLabel + " by " + saved.getDeliveryAgentName() + ".", "STATUS_UPDATED");

        return mapToDto(saved);
    }

    @Override
    public OrderDto markCodPayment(Long orderId, Long deliveryAgentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(deliveryAgentId)) {
            throw new InvalidRequestException("Access denied: You are not assigned to Order #" + orderId);
        }

        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus("PAID");
            Order saved = orderRepository.save(order);
            return mapToDto(saved);
        }
        return mapToDto(order);
    }

    @Override
    @Transactional
    public OrderDto cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (customerId != null && !order.getCustomerId().equals(customerId)) {
            throw new InvalidRequestException("Access denied: You are not authorized to cancel Order #" + orderId);
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new InvalidRequestException("Order #" + orderId + " is already cancelled.");
        }

        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new InvalidRequestException("Order #" + orderId + " has already been delivered and cannot be cancelled.");
        }

        if (order.getOrderStatus() == OrderStatus.OUT_FOR_DELIVERY) {
            throw new InvalidRequestException("Order #" + orderId + " is out for delivery. Please contact support or the delivery specialist.");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        // Process automatic refund to wallet if paid via WALLET
        if (order.getPaymentMethod() == PaymentMethod.WALLET && "PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            try {
                walletClient.addMoney(new AddMoneyRequest(
                        order.getCustomerId(),
                        order.getOrderAmount(),
                        "Refund for Cancelled Order #" + order.getOrderId()
                ));
                order.setRefundStatus("REFUNDED");
                order.setRefundReason("Order cancelled by customer - refunded to wallet");
            } catch (Exception ex) {
                log.error("Failed to automatically refund wallet for order #{}: {}", orderId, ex.getMessage());
                order.setRefundStatus("REFUND_FAILED");
            }
        }

        Order saved = orderRepository.save(order);

        // Restock inventory in Product Service
        for (OrderItem item : saved.getItems()) {
            if (item.getProductId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                try {
                    productClient.addStock(item.getProductId(), item.getQuantity());
                } catch (Exception ex) {
                    log.warn("Failed to restock product #{} on order cancellation: {}", item.getProductId(), ex.getMessage());
                }
            }
        }

        // In-App Notifications
        sendNotification(saved.getCustomerId(), "CUSTOMER", "Order Cancelled", "Your order #" + saved.getOrderId() + " has been successfully cancelled." + ("REFUNDED".equals(saved.getRefundStatus()) ? " ₹" + String.format("%.2f", saved.getOrderAmount()) + " refunded to your wallet." : ""), "ORDER_CANCELLED");
        for (OrderItem item : saved.getItems()) {
            if (item.getMerchantId() != null) {
                sendNotification(item.getMerchantId(), "MERCHANT", "Order Cancelled", "Order #" + saved.getOrderId() + " was cancelled by the customer.", "ORDER_CANCELLED");
            }
        }
        if (saved.getDeliveryAgentId() != null) {
            sendNotification(saved.getDeliveryAgentId(), "DELIVERY_AGENT", "Order Cancelled", "Order #" + saved.getOrderId() + " was cancelled by the customer.", "ORDER_CANCELLED");
        }
        notifyAdmins("Order Cancelled", "Order #" + saved.getOrderId() + " was cancelled by customer #" + saved.getCustomerId() + ".", "ORDER_CANCELLED");

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public OrderDto requestRefund(Long orderId, RefundRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (request != null && request.getCustomerId() != null && !order.getCustomerId().equals(request.getCustomerId())) {
            throw new InvalidRequestException("Access denied: You are not authorized to request a refund for Order #" + orderId);
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERED && order.getOrderStatus() != OrderStatus.CANCELLED) {
            throw new InvalidRequestException("Refunds can only be requested for Delivered or Cancelled orders.");
        }

        order.setRefundStatus("REQUESTED");
        if (request != null && request.getReason() != null) {
            order.setRefundReason(request.getReason());
        }

        Order saved = orderRepository.save(order);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Refund Request Submitted", "Your refund request for Order #" + saved.getOrderId() + " has been submitted and is under review.", "REFUND_REQUESTED");
        notifyAdmins("Refund Requested", "Customer #" + saved.getCustomerId() + " requested a refund for Order #" + saved.getOrderId() + ": " + request.getReason(), "REFUND_REQUESTED");

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getCurrentOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId).stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.DELIVERED && o.getOrderStatus() != OrderStatus.CANCELLED)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private void sendNotification(Long recipientId, String role, String title, String message, String type) {
        if (recipientId == null) return;
        try {
            notificationClient.createNotification(new CreateNotificationRequest(recipientId, role, title, message, type));
        } catch (Exception ex) {
            log.warn("Failed to dispatch in-app notification to recipient {}: {}", recipientId, ex.getMessage());
        }
    }

    private void notifyAdmins(String title, String message, String type) {
        try {
            List<UserProfileDto> admins = profileClient.getProfilesByRole("ADMIN");
            if (admins != null && !admins.isEmpty()) {
                for (UserProfileDto admin : admins) {
                    sendNotification(admin.getProfileId(), "ADMIN", title, message, type);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to notify admins: {}", ex.getMessage());
        }
    }

    private OrderDto mapToDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getOrderItemId(),
                        item.getProductId(),
                        item.getMerchantId(),
                        item.getProductName(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getSubTotal(),
                        item.getImageUrl()
                ))
                .collect(Collectors.toList());

        return OrderDto.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .orderDate(order.getOrderDate())
                .orderAmount(order.getOrderAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus() : "PAID")
                .orderStatus(order.getOrderStatus())
                .shippingAddress(order.getShippingAddress())
                .deliveryAgentId(order.getDeliveryAgentId())
                .deliveryAgentName(order.getDeliveryAgentName() != null ? order.getDeliveryAgentName() : "Unassigned")
                .assignedDate(order.getAssignedDate())
                .outForDeliveryDate(order.getOutForDeliveryDate())
                .deliveredDate(order.getDeliveredDate())
                .refundStatus(order.getRefundStatus() != null ? order.getRefundStatus() : "NONE")
                .refundReason(order.getRefundReason())
                .assignmentStatus(order.getAssignmentStatus() != null ? order.getAssignmentStatus() : "NONE")
                .assignmentRejectionReason(order.getAssignmentRejectionReason())
                .pickupStatus(order.getPickupStatus() != null ? order.getPickupStatus() : "PENDING")
                .pickupDate(order.getPickupDate())
                .deliveryOtp(order.getDeliveryOtp())
                .deliveryProofUrl(order.getDeliveryProofUrl())
                .deliveryFailureReason(order.getDeliveryFailureReason())
                .rescheduledDate(order.getRescheduledDate())
                .returnToMerchantStatus(order.getReturnToMerchantStatus() != null ? order.getReturnToMerchantStatus() : "NONE")
                .items(itemDtos)
                .build();
    }

    private boolean isMerchantOrder(Order order, Long merchantId) {
        if (merchantId == null) return true;
        return order.getItems().stream()
                .anyMatch(item -> item.getMerchantId() != null && item.getMerchantId().equals(merchantId));
    }

    @Override
    public OrderDto acceptOrder(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!isMerchantOrder(order, merchantId)) {
            throw new InvalidRequestException("Access denied: You do not have products in Order #" + orderId);
        }

        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new InvalidRequestException("Order #" + orderId + " cannot be accepted because current status is " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Order Accepted!", "Your order #" + saved.getOrderId() + " has been accepted by the merchant and is being prepared.", "ORDER_CONFIRMED");
        notifyAdmins("Order Accepted by Merchant", "Order #" + saved.getOrderId() + " was confirmed by merchant #" + merchantId + ".", "ORDER_CONFIRMED");

        return mapToDto(saved);
    }

    @Override
    public OrderDto rejectOrder(Long orderId, Long merchantId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!isMerchantOrder(order, merchantId)) {
            throw new InvalidRequestException("Access denied: You do not have products in Order #" + orderId);
        }

        if (order.getOrderStatus() != OrderStatus.PLACED && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidRequestException("Order #" + orderId + " cannot be rejected once in " + order.getOrderStatus() + " state.");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        String rejectionText = (reason != null && !reason.isBlank()) ? reason : "Item unavailable / Out of stock";
        order.setRefundReason("Rejected by merchant: " + rejectionText);

        // Process refund to customer wallet if order was paid with wallet or online
        if ((order.getPaymentMethod() == PaymentMethod.WALLET || order.getPaymentMethod() == PaymentMethod.ONLINE) && "PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            try {
                walletClient.addMoney(new AddMoneyRequest(
                        order.getCustomerId(),
                        order.getOrderAmount(),
                        "Refund for Merchant Rejected Order #" + order.getOrderId()
                ));
                order.setRefundStatus("REFUNDED");
            } catch (Exception ex) {
                log.error("Failed to automatically refund wallet for rejected order #{}: {}", orderId, ex.getMessage());
                order.setRefundStatus("REFUND_FAILED");
            }
        }

        Order saved = orderRepository.save(order);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Order Rejected", "Your order #" + saved.getOrderId() + " could not be fulfilled: " + rejectionText + ("REFUNDED".equals(saved.getRefundStatus()) ? " ₹" + String.format("%.2f", saved.getOrderAmount()) + " has been refunded to your wallet." : ""), "ORDER_CANCELLED");
        notifyAdmins("Order Rejected", "Order #" + saved.getOrderId() + " was rejected by merchant #" + merchantId + " (" + rejectionText + ").", "ORDER_CANCELLED");

        return mapToDto(saved);
    }

    @Override
    public OrderDto processOrder(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!isMerchantOrder(order, merchantId)) {
            throw new InvalidRequestException("Access denied: You do not own items in Order #" + orderId);
        }

        if (order.getOrderStatus() == OrderStatus.PLACED) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
            order = orderRepository.save(order);
        }

        sendNotification(order.getCustomerId(), "CUSTOMER", "Order in Process", "Your order #" + order.getOrderId() + " is now being processed by the merchant.", "STATUS_UPDATED");
        return mapToDto(order);
    }

    @Override
    public OrderDto packOrder(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!isMerchantOrder(order, merchantId)) {
            throw new InvalidRequestException("Access denied: You do not own items in Order #" + orderId);
        }

        if (order.getOrderStatus() == OrderStatus.PLACED) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
            order = orderRepository.save(order);
        }

        sendNotification(order.getCustomerId(), "CUSTOMER", "Order Packed", "Your items for Order #" + order.getOrderId() + " have been packed and are ready for pickup.", "ORDER_PACKED");
        notifyAdmins("Order Packed", "Order #" + order.getOrderId() + " has been packed by merchant #" + merchantId + ".", "ORDER_PACKED");

        return mapToDto(order);
    }

    @Override
    public OrderDto readyForPickup(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!isMerchantOrder(order, merchantId)) {
            throw new InvalidRequestException("Access denied: You do not own items in Order #" + orderId);
        }

        if (order.getOrderStatus() == OrderStatus.PLACED) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
            order = orderRepository.save(order);
        }

        sendNotification(order.getCustomerId(), "CUSTOMER", "Order Ready for Pickup", "Your order #" + order.getOrderId() + " is packaged and ready for courier pickup.", "READY_FOR_PICKUP");
        notifyAdmins("Order Ready for Pickup", "Order #" + order.getOrderId() + " is packed and ready for delivery agent pickup.", "READY_FOR_PICKUP");

        if (order.getDeliveryAgentId() != null) {
            sendNotification(order.getDeliveryAgentId(), "DELIVERY_AGENT", "Order Ready for Pickup", "Order #" + order.getOrderId() + " is ready for pickup at merchant facility.", "READY_FOR_PICKUP");
        }

        return mapToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getMerchantOrders(Long merchantId, String statusFilter) {
        List<Order> orders;
        if (merchantId == null) {
            orders = orderRepository.findAllByOrderByOrderDateDesc();
        } else {
            orders = orderRepository.findOrdersByMerchantId(merchantId);
        }

        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            orders = orders.stream()
                    .filter(o -> o.getOrderStatus() != null && o.getOrderStatus().name().equalsIgnoreCase(statusFilter))
                    .collect(Collectors.toList());
        }

        return orders.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public ReturnRequestDto createReturnRequest(Long orderId, Long customerId, Long productId, Integer quantity, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (customerId != null && !order.getCustomerId().equals(customerId)) {
            throw new InvalidRequestException("Access denied: You do not own Order #" + orderId);
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new InvalidRequestException("Returns can only be requested for delivered orders.");
        }

        OrderItem matchedItem = order.getItems().stream()
                .filter(i -> productId == null || i.getProductId().equals(productId))
                .findFirst()
                .orElse(order.getItems().isEmpty() ? null : order.getItems().get(0));

        if (matchedItem == null) {
            throw new InvalidRequestException("No product items found for Order #" + orderId);
        }

        int returnQty = (quantity != null && quantity > 0) ? Math.min(quantity, matchedItem.getQuantity()) : matchedItem.getQuantity();
        double refundAmt = (matchedItem.getPrice() != null ? matchedItem.getPrice() : 0.0) * returnQty;
        Long merchantId = matchedItem.getMerchantId() != null ? matchedItem.getMerchantId() : 0L;

        ReturnRequest returnReq = ReturnRequest.builder()
                .orderId(orderId)
                .productId(matchedItem.getProductId())
                .merchantId(merchantId)
                .customerId(order.getCustomerId())
                .productName(matchedItem.getProductName())
                .quantity(returnQty)
                .refundAmount(refundAmt)
                .reason(reason != null ? reason : "Product Return / Refund")
                .status("REQUESTED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ReturnRequest saved = returnRequestRepository.save(returnReq);

        sendNotification(merchantId, "MERCHANT", "New Return Request", "Customer requested return for '" + matchedItem.getProductName() + "' (Order #" + orderId + ").", "RETURN_REQUESTED");
        sendNotification(order.getCustomerId(), "CUSTOMER", "Return Request Submitted", "Your return request for Order #" + orderId + " has been submitted to the merchant.", "RETURN_REQUESTED");

        return mapToReturnDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnRequestDto> getMerchantReturnRequests(Long merchantId) {
        if (merchantId == null) {
            return returnRequestRepository.findAll().stream()
                    .map(this::mapToReturnDto)
                    .collect(Collectors.toList());
        }
        return returnRequestRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(this::mapToReturnDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnRequestDto> getCustomerReturnRequests(Long customerId) {
        return returnRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToReturnDto)
                .collect(Collectors.toList());
    }

    @Override
    public ReturnRequestDto approveReturn(Long returnId, Long merchantId) {
        ReturnRequest req = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with id: " + returnId));

        if (merchantId != null && req.getMerchantId() != null && !req.getMerchantId().equals(merchantId)) {
            throw new InvalidRequestException("Access denied: You do not own the return request #" + returnId);
        }

        if (!"REQUESTED".equalsIgnoreCase(req.getStatus())) {
            throw new InvalidRequestException("Return request #" + returnId + " is in status " + req.getStatus() + " and cannot be approved.");
        }

        req.setStatus("APPROVED");
        req.setUpdatedAt(LocalDateTime.now());
        ReturnRequest saved = returnRequestRepository.save(req);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Return Request Approved", "Your return request for '" + saved.getProductName() + "' has been approved by the merchant. Please send the product back.", "RETURN_APPROVED");

        return mapToReturnDto(saved);
    }

    @Override
    public ReturnRequestDto rejectReturn(Long returnId, Long merchantId, String rejectionReason) {
        ReturnRequest req = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with id: " + returnId));

        if (merchantId != null && req.getMerchantId() != null && !req.getMerchantId().equals(merchantId)) {
            throw new InvalidRequestException("Access denied: You do not own the return request #" + returnId);
        }

        if (!"REQUESTED".equalsIgnoreCase(req.getStatus())) {
            throw new InvalidRequestException("Return request #" + returnId + " is in status " + req.getStatus() + " and cannot be rejected.");
        }

        req.setStatus("REJECTED");
        req.setRejectionReason(rejectionReason != null ? rejectionReason : "Return criteria not met");
        req.setUpdatedAt(LocalDateTime.now());
        ReturnRequest saved = returnRequestRepository.save(req);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Return Request Rejected", "Your return request for '" + saved.getProductName() + "' was rejected: " + req.getRejectionReason(), "RETURN_REJECTED");

        return mapToReturnDto(saved);
    }

    @Override
    public ReturnRequestDto receiveReturnedProduct(Long returnId, Long merchantId) {
        ReturnRequest req = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with id: " + returnId));

        if (merchantId != null && req.getMerchantId() != null && !req.getMerchantId().equals(merchantId)) {
            throw new InvalidRequestException("Access denied: You do not own the return request #" + returnId);
        }

        if (!"APPROVED".equalsIgnoreCase(req.getStatus())) {
            throw new InvalidRequestException("Return request #" + returnId + " must be in APPROVED state before marking received (Current: " + req.getStatus() + ").");
        }

        req.setStatus("RECEIVED");
        req.setUpdatedAt(LocalDateTime.now());
        ReturnRequest saved = returnRequestRepository.save(req);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Returned Product Received", "Merchant has received the returned item '" + saved.getProductName() + "'. Refund will be processed.", "RETURN_RECEIVED");

        return mapToReturnDto(saved);
    }

    @Override
    public ReturnRequestDto restockAndCompleteReturn(Long returnId, Long merchantId) {
        ReturnRequest req = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with id: " + returnId));

        if (merchantId != null && req.getMerchantId() != null && !req.getMerchantId().equals(merchantId)) {
            throw new InvalidRequestException("Access denied: You do not own the return request #" + returnId);
        }

        if ("COMPLETED".equalsIgnoreCase(req.getStatus())) {
            throw new InvalidRequestException("Return request #" + returnId + " has already been completed and restocked.");
        }

        // 1. Restock item in Product Service
        try {
            if (req.getProductId() != null && req.getQuantity() != null && req.getQuantity() > 0) {
                productClient.addStock(req.getProductId(), req.getQuantity());
                log.info("Restocked {} units of Product #{} after return #{}", req.getQuantity(), req.getProductId(), returnId);
            }
        } catch (Exception ex) {
            log.warn("Failed to restock product #{} during return processing: {}", req.getProductId(), ex.getMessage());
        }

        // 2. Issue Refund to Customer Wallet
        if (req.getRefundAmount() != null && req.getRefundAmount() > 0) {
            try {
                walletClient.addMoney(new AddMoneyRequest(
                        req.getCustomerId(),
                        req.getRefundAmount(),
                        "Refund for Returned Product '" + req.getProductName() + "' (Return #" + returnId + ")"
                ));
            } catch (Exception ex) {
                log.error("Failed to issue wallet refund for return #{}: {}", returnId, ex.getMessage());
            }
        }

        req.setStatus("COMPLETED");
        req.setUpdatedAt(LocalDateTime.now());
        ReturnRequest saved = returnRequestRepository.save(req);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Return Completed & Refunded", "Your return for '" + saved.getProductName() + "' is completed. ₹" + String.format("%.2f", saved.getRefundAmount()) + " has been refunded to your wallet.", "RETURN_COMPLETED");
        sendNotification(saved.getMerchantId(), "MERCHANT", "Return Restocked & Completed", "Return #" + returnId + " for '" + saved.getProductName() + "' restocked (" + saved.getQuantity() + " units) and completed.", "RETURN_COMPLETED");

        return mapToReturnDto(saved);
    }

    private ReturnRequestDto mapToReturnDto(ReturnRequest req) {
        String customerName = "Customer #" + req.getCustomerId();
        try {
            UserProfileDto profile = profileClient.getProfile(req.getCustomerId());
            if (profile != null && profile.getFullName() != null) {
                customerName = profile.getFullName();
            }
        } catch (Exception ignored) {
        }

        return ReturnRequestDto.builder()
                .returnId(req.getReturnId())
                .orderId(req.getOrderId())
                .productId(req.getProductId())
                .merchantId(req.getMerchantId())
                .customerId(req.getCustomerId())
                .customerName(customerName)
                .productName(req.getProductName())
                .quantity(req.getQuantity())
                .refundAmount(req.getRefundAmount())
                .reason(req.getReason())
                .status(req.getStatus())
                .rejectionReason(req.getRejectionReason())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .build();
    }

    @Override
    public OrderDto acceptAssignment(Long orderId, Long deliveryAgentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(deliveryAgentId)) {
            throw new InvalidRequestException("Access denied: You are not assigned to Order #" + orderId);
        }

        order.setAssignmentStatus("ACCEPTED");
        Order saved = orderRepository.save(order);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Delivery Agent Confirmed", "Delivery agent " + saved.getDeliveryAgentName() + " has accepted the assignment for Order #" + saved.getOrderId() + ".", "ORDER_ASSIGNED");
        notifyAdmins("Assignment Accepted", "Agent " + saved.getDeliveryAgentName() + " accepted assignment for Order #" + saved.getOrderId() + ".", "ORDER_ASSIGNED");

        return mapToDto(saved);
    }

    @Override
    public OrderDto rejectAssignment(Long orderId, Long deliveryAgentId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(deliveryAgentId)) {
            throw new InvalidRequestException("Access denied: You are not assigned to Order #" + orderId);
        }

        String rejectionReason = (reason != null && !reason.isBlank()) ? reason : "Agent unavailable";
        String previousAgentName = order.getDeliveryAgentName();

        order.setAssignmentStatus("REJECTED");
        order.setAssignmentRejectionReason(rejectionReason);
        order.setDeliveryAgentId(null);
        order.setDeliveryAgentName(null);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);

        notifyAdmins("Assignment Rejected", "Agent " + previousAgentName + " rejected assignment for Order #" + saved.getOrderId() + ": " + rejectionReason + ". Please reassign.", "ASSIGNMENT_REJECTED");

        return mapToDto(saved);
    }

    @Override
    public OrderDto pickupPackage(Long orderId, Long deliveryAgentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(deliveryAgentId)) {
            throw new InvalidRequestException("Access denied: You are not assigned to Order #" + orderId);
        }

        order.setPickupStatus("PICKED_UP");
        order.setPickupDate(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        for (OrderItem item : saved.getItems()) {
            if (item.getMerchantId() != null) {
                sendNotification(item.getMerchantId(), "MERCHANT", "Package Picked Up", "Agent " + saved.getDeliveryAgentName() + " has arrived and picked up package for Order #" + saved.getOrderId() + ".", "PACKAGE_PICKED_UP");
            }
        }

        return mapToDto(saved);
    }

    @Override
    public OrderDto confirmPickup(Long orderId, Long deliveryAgentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(deliveryAgentId)) {
            throw new InvalidRequestException("Access denied: You are not assigned to Order #" + orderId);
        }

        order.setPickupStatus("CONFIRMED");
        order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setOutForDeliveryDate(LocalDateTime.now());

        // Generate 6-digit delivery OTP for customer verification
        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        order.setDeliveryOtp(otp);

        Order saved = orderRepository.save(order);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Out for Delivery!", "Your Order #" + saved.getOrderId() + " is out for delivery with " + saved.getDeliveryAgentName() + ". Share OTP " + otp + " with the agent upon delivery.", "OUT_FOR_DELIVERY");
        notifyAdmins("Out for Delivery", "Order #" + saved.getOrderId() + " is in transit with " + saved.getDeliveryAgentName() + ".", "OUT_FOR_DELIVERY");

        return mapToDto(saved);
    }

    @Override
    public OrderDto uploadDeliveryProof(Long orderId, Long deliveryAgentId, String proofUrl) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(deliveryAgentId)) {
            throw new InvalidRequestException("Access denied: You are not assigned to Order #" + orderId);
        }

        order.setDeliveryProofUrl(proofUrl);
        Order saved = orderRepository.save(order);
        return mapToDto(saved);
    }

    @Override
    public OrderDto verifyDeliveryOtp(Long orderId, Long deliveryAgentId, String otp) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(deliveryAgentId)) {
            throw new InvalidRequestException("Access denied: You are not assigned to Order #" + orderId);
        }

        if (order.getOrderStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new InvalidRequestException("OTP can only be verified when order is OUT_FOR_DELIVERY.");
        }

        if (order.getDeliveryOtp() == null || !order.getDeliveryOtp().equals(otp != null ? otp.trim() : "")) {
            throw new InvalidRequestException("Invalid Delivery OTP. Please ask the customer for the correct 6-digit OTP.");
        }

        // If COD, mark payment status PAID upon successful delivery
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus("PAID");
        }

        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setDeliveredDate(LocalDateTime.now());
        order.setDeliveryOtp(null); // prevent reuse

        Order saved = orderRepository.save(order);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Order Delivered Successfully!", "Your Order #" + saved.getOrderId() + " has been delivered. Thank you for shopping with EShoppingZone!", "ORDER_DELIVERED");
        notifyAdmins("Order Delivered", "Order #" + saved.getOrderId() + " was successfully delivered by " + saved.getDeliveryAgentName() + ".", "ORDER_DELIVERED");

        return mapToDto(saved);
    }

    @Override
    public OrderDto markDeliveryFailed(Long orderId, Long deliveryAgentId, String failureReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(deliveryAgentId)) {
            throw new InvalidRequestException("Access denied: You are not assigned to Order #" + orderId);
        }

        String reason = (failureReason != null && !failureReason.isBlank()) ? failureReason : "Customer unavailable / Address unreachable";
        order.setDeliveryFailureReason(reason);
        Order saved = orderRepository.save(order);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Delivery Attempt Failed", "Delivery attempt for Order #" + saved.getOrderId() + " failed: " + reason + ". We will attempt rescheduling or returning.", "DELIVERY_FAILED");
        notifyAdmins("Delivery Attempt Failed", "Delivery attempt for Order #" + saved.getOrderId() + " failed: " + reason + ".", "DELIVERY_FAILED");

        return mapToDto(saved);
    }

    @Override
    public OrderDto rescheduleDelivery(Long orderId, Long deliveryAgentId, java.time.LocalDateTime newDate) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(deliveryAgentId)) {
            throw new InvalidRequestException("Access denied: You are not assigned to Order #" + orderId);
        }

        java.time.LocalDateTime schedule = (newDate != null) ? newDate : java.time.LocalDateTime.now().plusDays(1);
        order.setRescheduledDate(schedule);
        order.setDeliveryFailureReason(null);
        Order saved = orderRepository.save(order);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Delivery Rescheduled", "Your delivery for Order #" + saved.getOrderId() + " has been rescheduled to " + schedule.toLocalDate() + ".", "DELIVERY_RESCHEDULED");

        return mapToDto(saved);
    }

    @Override
    public OrderDto returnPackageToMerchant(Long orderId, Long deliveryAgentId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(deliveryAgentId)) {
            throw new InvalidRequestException("Access denied: You are not assigned to Order #" + orderId);
        }

        order.setReturnToMerchantStatus("RETURNED_TO_MERCHANT");
        order.setOrderStatus(OrderStatus.CANCELLED);
        String returnReason = (reason != null && !reason.isBlank()) ? reason : "Undeliverable / Customer refused";
        order.setRefundReason("Returned to merchant by delivery agent: " + returnReason);

        // Refund customer if already paid
        if ((order.getPaymentMethod() == PaymentMethod.WALLET || order.getPaymentMethod() == PaymentMethod.ONLINE) && "PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            try {
                walletClient.addMoney(new AddMoneyRequest(
                        order.getCustomerId(),
                        order.getOrderAmount(),
                        "Refund for Undeliverable Order #" + order.getOrderId()
                ));
                order.setRefundStatus("REFUNDED");
            } catch (Exception ex) {
                log.error("Failed to refund wallet on package return for order #{}: {}", orderId, ex.getMessage());
                order.setRefundStatus("REFUND_FAILED");
            }
        }

        Order saved = orderRepository.save(order);

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Order Returned to Merchant", "Order #" + saved.getOrderId() + " was returned to the merchant (" + returnReason + ")." + ("REFUNDED".equals(saved.getRefundStatus()) ? " Your wallet has been refunded." : ""), "ORDER_CANCELLED");
        for (OrderItem item : saved.getItems()) {
            if (item.getMerchantId() != null) {
                sendNotification(item.getMerchantId(), "MERCHANT", "Package Returned", "Package for Order #" + saved.getOrderId() + " was returned to your store by delivery agent.", "PACKAGE_RETURNED");
            }
        }
        notifyAdmins("Package Returned to Merchant", "Package for Order #" + saved.getOrderId() + " was returned to merchant by agent " + saved.getDeliveryAgentName() + ".", "PACKAGE_RETURNED");

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> searchOrders(String query, String status) {
        List<Order> all = orderRepository.findAll();
        return all.stream()
                .filter(o -> {
                    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(o.getOrderStatus().name())) {
                            return false;
                        }
                    }
                    if (query != null && !query.isBlank()) {
                        String q = query.toLowerCase().trim();
                        boolean matchId = String.valueOf(o.getOrderId()).contains(q);
                        boolean matchCust = o.getCustomerId() != null && String.valueOf(o.getCustomerId()).contains(q);
                        boolean matchAgent = o.getDeliveryAgentName() != null && o.getDeliveryAgentName().toLowerCase().contains(q);
                        boolean matchAddress = o.getShippingAddress() != null && o.getShippingAddress().getFullName() != null && o.getShippingAddress().getFullName().toLowerCase().contains(q);
                        return matchId || matchCust || matchAgent || matchAddress;
                    }
                    return true;
                })
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDto adminCancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new InvalidRequestException("Cannot cancel an order that has already been DELIVERED.");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        String cancelReason = (reason != null && !reason.isBlank()) ? reason : "Administrative cancellation";
        order.setRefundReason(cancelReason);

        if ((order.getPaymentMethod() == PaymentMethod.WALLET || order.getPaymentMethod() == PaymentMethod.ONLINE) && "PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            try {
                walletClient.addMoney(new AddMoneyRequest(
                        order.getCustomerId(),
                        order.getOrderAmount(),
                        "Refund for Admin Cancelled Order #" + order.getOrderId()
                ));
                order.setRefundStatus("REFUNDED");
            } catch (Exception ex) {
                log.error("Failed to refund wallet for admin cancelled order #{}: {}", orderId, ex.getMessage());
                order.setRefundStatus("REFUND_FAILED");
            }
        }

        Order saved = orderRepository.save(order);

        // Restock inventory in Product Service
        for (OrderItem item : saved.getItems()) {
            if (item.getProductId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                try {
                    productClient.addStock(item.getProductId(), item.getQuantity());
                } catch (Exception ex) {
                    log.warn("Failed to restock product #{} on admin order cancellation: {}", item.getProductId(), ex.getMessage());
                }
            }
        }

        sendNotification(saved.getCustomerId(), "CUSTOMER", "Order Cancelled by Administrator", "Your order #" + saved.getOrderId() + " was cancelled (" + cancelReason + ")." + ("REFUNDED".equals(saved.getRefundStatus()) ? " Wallet refunded." : ""), "ORDER_CANCELLED");
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public CustomerComplaintDto createComplaint(CustomerComplaintDto dto) {
        CustomerComplaint complaint = CustomerComplaint.builder()
                .orderId(dto.getOrderId())
                .customerId(dto.getCustomerId())
                .customerName(dto.getCustomerName())
                .merchantId(dto.getMerchantId())
                .subject(dto.getSubject())
                .description(dto.getDescription())
                .complaintType(dto.getComplaintType() != null ? dto.getComplaintType() : "COMPLAINT")
                .status("OPEN")
                .createdAt(LocalDateTime.now())
                .build();
        CustomerComplaint saved = complaintRepository.save(complaint);
        notifyAdmins("New Complaint Filed", "Complaint #" + saved.getComplaintId() + " regarding Order #" + saved.getOrderId() + " was filed: " + saved.getSubject(), "NEW_COMPLAINT");
        return mapToComplaintDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerComplaintDto> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToComplaintDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerComplaintDto getComplaintById(Long complaintId) {
        CustomerComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));
        return mapToComplaintDto(complaint);
    }

    @Override
    @Transactional
    public CustomerComplaintDto updateComplaintStatus(Long complaintId, String status, String resolution) {
        CustomerComplaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));
        complaint.setStatus(status);
        if (resolution != null && !resolution.isBlank()) {
            complaint.setResolution(resolution);
        }
        if ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            complaint.setResolvedAt(LocalDateTime.now());
        }
        CustomerComplaint saved = complaintRepository.save(complaint);
        if (saved.getCustomerId() != null) {
            sendNotification(saved.getCustomerId(), "CUSTOMER", "Complaint Updated", "Your complaint #" + saved.getComplaintId() + " status is now " + status + (resolution != null ? ": " + resolution : ""), "COMPLAINT_UPDATE");
        }
        return mapToComplaintDto(saved);
    }

    private CustomerComplaintDto mapToComplaintDto(CustomerComplaint c) {
        return CustomerComplaintDto.builder()
                .complaintId(c.getComplaintId())
                .orderId(c.getOrderId())
                .customerId(c.getCustomerId())
                .customerName(c.getCustomerName())
                .merchantId(c.getMerchantId())
                .subject(c.getSubject())
                .description(c.getDescription())
                .complaintType(c.getComplaintType())
                .status(c.getStatus())
                .resolution(c.getResolution())
                .createdAt(c.getCreatedAt())
                .resolvedAt(c.getResolvedAt())
                .build();
    }
}
