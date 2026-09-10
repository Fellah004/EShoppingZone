package com.eshoppingzone.order.service;

import com.eshoppingzone.order.dto.*;

import java.util.List;

public interface OrderService {

    OrderDto createOrder(CreateOrderRequest request);

    OrderDto getOrderById(Long orderId);

    List<OrderDto> getOrdersByCustomerId(Long customerId);

    List<OrderDto> getAllOrders();

    List<OrderDto> getOrdersByDeliveryAgentId(Long deliveryAgentId);

    List<MerchantSaleDto> getMerchantSales(Long merchantId);

    OrderDto updateOrderStatus(Long orderId, OrderStatusUpdateRequest request);

    OrderDto assignDeliveryAgent(Long orderId, AssignDeliveryAgentRequest request);

    OrderDto updateDeliveryStatus(Long orderId, UpdateDeliveryStatusRequest request);

    OrderDto markCodPayment(Long orderId, Long deliveryAgentId);

    OrderDto cancelOrder(Long orderId, Long customerId);

    OrderDto requestRefund(Long orderId, RefundRequest request);

    List<OrderDto> getCurrentOrdersByCustomerId(Long customerId);

    OrderDto acceptOrder(Long orderId, Long merchantId);

    OrderDto rejectOrder(Long orderId, Long merchantId, String reason);

    OrderDto processOrder(Long orderId, Long merchantId);

    OrderDto packOrder(Long orderId, Long merchantId);

    OrderDto readyForPickup(Long orderId, Long merchantId);

    List<OrderDto> getMerchantOrders(Long merchantId, String statusFilter);

    ReturnRequestDto createReturnRequest(Long orderId, Long customerId, Long productId, Integer quantity, String reason);

    List<ReturnRequestDto> getMerchantReturnRequests(Long merchantId);

    List<ReturnRequestDto> getCustomerReturnRequests(Long customerId);

    ReturnRequestDto approveReturn(Long returnId, Long merchantId);

    ReturnRequestDto rejectReturn(Long returnId, Long merchantId, String rejectionReason);

    ReturnRequestDto receiveReturnedProduct(Long returnId, Long merchantId);

    ReturnRequestDto restockAndCompleteReturn(Long returnId, Long merchantId);

    OrderDto acceptAssignment(Long orderId, Long deliveryAgentId);

    OrderDto rejectAssignment(Long orderId, Long deliveryAgentId, String reason);

    OrderDto pickupPackage(Long orderId, Long deliveryAgentId);

    OrderDto confirmPickup(Long orderId, Long deliveryAgentId);

    OrderDto uploadDeliveryProof(Long orderId, Long deliveryAgentId, String proofUrl);

    OrderDto verifyDeliveryOtp(Long orderId, Long deliveryAgentId, String otp);

    OrderDto markDeliveryFailed(Long orderId, Long deliveryAgentId, String failureReason);

    OrderDto rescheduleDelivery(Long orderId, Long deliveryAgentId, java.time.LocalDateTime newDate);

    OrderDto returnPackageToMerchant(Long orderId, Long deliveryAgentId, String reason);

    List<OrderDto> searchOrders(String query, String status);

    OrderDto adminCancelOrder(Long orderId, String reason);

    CustomerComplaintDto createComplaint(CustomerComplaintDto complaintDto);

    List<CustomerComplaintDto> getAllComplaints();

    CustomerComplaintDto getComplaintById(Long complaintId);

    CustomerComplaintDto updateComplaintStatus(Long complaintId, String status, String resolution);
}
