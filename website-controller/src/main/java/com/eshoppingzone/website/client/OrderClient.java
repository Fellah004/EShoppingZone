package com.eshoppingzone.website.client;

import com.eshoppingzone.website.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "order-service", url = "${services.order-service.url:http://localhost:8084}")
public interface OrderClient {

    @PostMapping("/api/orders")
    OrderDto createOrder(@RequestBody CreateOrderRequest request);

    @GetMapping("/api/orders/all")
    List<OrderDto> getAllOrders();

    @GetMapping("/api/orders/{id}")
    OrderDto getOrderById(@PathVariable("id") Long id);

    @GetMapping("/api/orders/customer/{customerId}")
    List<OrderDto> getOrdersByCustomerId(@PathVariable("customerId") Long customerId);

    @GetMapping("/api/orders/delivery-agent/{deliveryAgentId}")
    List<OrderDto> getOrdersByDeliveryAgentId(@PathVariable("deliveryAgentId") Long deliveryAgentId);

    @GetMapping("/api/orders/merchant/{merchantId}")
    List<MerchantSaleDto> getMerchantSales(@PathVariable("merchantId") Long merchantId);

    @PutMapping("/api/orders/{id}/assign")
    OrderDto assignDeliveryAgent(@PathVariable("id") Long id, @RequestBody AssignDeliveryAgentRequest request);

    @PutMapping("/api/orders/{id}/delivery-status")
    OrderDto updateDeliveryStatus(@PathVariable("id") Long id, @RequestBody UpdateDeliveryStatusRequest request);

    @PutMapping("/api/orders/{id}/cod-payment")
    OrderDto markCodPayment(@PathVariable("id") Long id, @RequestParam("deliveryAgentId") Long deliveryAgentId);

    @PutMapping("/api/orders/{id}/cancel")
    OrderDto cancelOrder(@PathVariable("id") Long id, @RequestParam("customerId") Long customerId);

    @PostMapping("/api/orders/{id}/refund")
    OrderDto requestRefund(@PathVariable("id") Long id, @RequestBody RefundRequest request);

    @GetMapping("/api/orders/customer/{customerId}/current")
    List<OrderDto> getCurrentOrdersByCustomerId(@PathVariable("customerId") Long customerId);

    @PostMapping("/api/orders/{id}/accept")
    OrderDto acceptOrder(@PathVariable("id") Long id, @RequestParam("merchantId") Long merchantId);

    @PostMapping("/api/orders/{id}/reject")
    OrderDto rejectOrder(@PathVariable("id") Long id, @RequestParam("merchantId") Long merchantId, @RequestParam(value = "reason", required = false) String reason);

    @PostMapping("/api/orders/{id}/process")
    OrderDto processOrder(@PathVariable("id") Long id, @RequestParam("merchantId") Long merchantId);

    @PostMapping("/api/orders/{id}/pack")
    OrderDto packOrder(@PathVariable("id") Long id, @RequestParam("merchantId") Long merchantId);

    @PostMapping("/api/orders/{id}/ready-for-pickup")
    OrderDto readyForPickup(@PathVariable("id") Long id, @RequestParam("merchantId") Long merchantId);

    @GetMapping("/api/orders/merchant/{merchantId}/list")
    List<OrderDto> getMerchantOrders(@PathVariable("merchantId") Long merchantId, @RequestParam(value = "status", required = false) String status);

    @PostMapping("/api/orders/returns")
    ReturnRequestDto createReturnRequest(
            @RequestParam("orderId") Long orderId,
            @RequestParam("customerId") Long customerId,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam("reason") String reason);

    @GetMapping("/api/orders/returns/merchant/{merchantId}")
    List<ReturnRequestDto> getMerchantReturnRequests(@PathVariable("merchantId") Long merchantId);

    @GetMapping("/api/orders/returns/customer/{customerId}")
    List<ReturnRequestDto> getCustomerReturnRequests(@PathVariable("customerId") Long customerId);

    @PostMapping("/api/orders/returns/{returnId}/approve")
    ReturnRequestDto approveReturn(@PathVariable("returnId") Long returnId, @RequestParam("merchantId") Long merchantId);

    @PostMapping("/api/orders/returns/{returnId}/reject")
    ReturnRequestDto rejectReturn(@PathVariable("returnId") Long returnId, @RequestParam("merchantId") Long merchantId, @RequestParam(value = "reason", required = false) String reason);

    @PostMapping("/api/orders/returns/{returnId}/receive")
    ReturnRequestDto receiveReturnedProduct(@PathVariable("returnId") Long returnId, @RequestParam("merchantId") Long merchantId);

    @PostMapping("/api/orders/returns/{returnId}/restock")
    ReturnRequestDto restockAndCompleteReturn(@PathVariable("returnId") Long returnId, @RequestParam("merchantId") Long merchantId);

    @PostMapping("/api/orders/{id}/accept-assignment")
    OrderDto acceptAssignment(@PathVariable("id") Long id, @RequestParam("deliveryAgentId") Long deliveryAgentId);

    @PostMapping("/api/orders/{id}/reject-assignment")
    OrderDto rejectAssignment(@PathVariable("id") Long id, @RequestParam("deliveryAgentId") Long deliveryAgentId, @RequestParam(value = "reason", required = false) String reason);

    @PostMapping("/api/orders/{id}/pickup")
    OrderDto pickupPackage(@PathVariable("id") Long id, @RequestParam("deliveryAgentId") Long deliveryAgentId);

    @PostMapping("/api/orders/{id}/confirm-pickup")
    OrderDto confirmPickup(@PathVariable("id") Long id, @RequestParam("deliveryAgentId") Long deliveryAgentId);

    @PostMapping("/api/orders/{id}/delivery-proof")
    OrderDto uploadDeliveryProof(@PathVariable("id") Long id, @RequestParam("deliveryAgentId") Long deliveryAgentId, @RequestParam("proofUrl") String proofUrl);

    @PostMapping("/api/orders/{id}/verify-otp")
    OrderDto verifyDeliveryOtp(@PathVariable("id") Long id, @RequestParam("deliveryAgentId") Long deliveryAgentId, @RequestParam("otp") String otp);

    @PostMapping("/api/orders/{id}/failed")
    OrderDto markDeliveryFailed(@PathVariable("id") Long id, @RequestParam("deliveryAgentId") Long deliveryAgentId, @RequestParam(value = "reason", required = false) String reason);

    @PostMapping("/api/orders/{id}/reschedule")
    OrderDto rescheduleDelivery(@PathVariable("id") Long id, @RequestParam("deliveryAgentId") Long deliveryAgentId, @RequestParam(value = "newDate", required = false) String newDate);

    @PostMapping("/api/orders/{id}/return-to-merchant")
    OrderDto returnPackageToMerchant(@PathVariable("id") Long id, @RequestParam("deliveryAgentId") Long deliveryAgentId, @RequestParam(value = "reason", required = false) String reason);

    @GetMapping("/api/orders/search")
    List<OrderDto> searchOrders(@RequestParam(value = "query", required = false) String query, @RequestParam(value = "status", required = false) String status);

    @PostMapping("/api/orders/{id}/admin-cancel")
    OrderDto adminCancelOrder(@PathVariable("id") Long id, @RequestParam(value = "reason", required = false) String reason);

    @PostMapping("/api/orders/complaints")
    CustomerComplaintDto createComplaint(@RequestBody CustomerComplaintDto complaintDto);

    @GetMapping("/api/orders/complaints")
    List<CustomerComplaintDto> getAllComplaints();

    @GetMapping("/api/orders/complaints/{id}")
    CustomerComplaintDto getComplaintById(@PathVariable("id") Long id);

    @PostMapping("/api/orders/complaints/{id}/status")
    CustomerComplaintDto updateComplaintStatus(@PathVariable("id") Long id, @RequestParam("status") String status, @RequestParam(value = "resolution", required = false) String resolution);
}
