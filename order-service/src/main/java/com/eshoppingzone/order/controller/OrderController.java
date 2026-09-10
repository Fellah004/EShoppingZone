package com.eshoppingzone.order.controller;

import com.eshoppingzone.order.dto.*;
import com.eshoppingzone.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/orders", "/api/order"})
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderDto order = orderService.createOrder(request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderDto>> getOrdersByCustomerId(@PathVariable("customerId") Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }

    @GetMapping("/delivery-agent/{deliveryAgentId}")
    public ResponseEntity<List<OrderDto>> getOrdersByDeliveryAgentId(@PathVariable("deliveryAgentId") Long deliveryAgentId) {
        return ResponseEntity.ok(orderService.getOrdersByDeliveryAgentId(deliveryAgentId));
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<MerchantSaleDto>> getMerchantSales(@PathVariable("merchantId") Long merchantId) {
        return ResponseEntity.ok(orderService.getMerchantSales(merchantId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    @PutMapping({"/{id}/assign", "/{id}/assign-agent", "/{id}/assign-delivery-agent"})
    public ResponseEntity<OrderDto> assignDeliveryAgent(
            @PathVariable("id") Long id,
            @Valid @RequestBody AssignDeliveryAgentRequest request) {
        return ResponseEntity.ok(orderService.assignDeliveryAgent(id, request));
    }

    @PutMapping("/{id}/delivery-status")
    public ResponseEntity<OrderDto> updateDeliveryStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateDeliveryStatusRequest request) {
        return ResponseEntity.ok(orderService.updateDeliveryStatus(id, request));
    }

    @RequestMapping(value = {"/{id}/cod-payment", "/{id}/mark-cod-paid"}, method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> markCodPayment(
            @PathVariable("id") Long id,
            @RequestParam("deliveryAgentId") Long deliveryAgentId) {
        return ResponseEntity.ok(orderService.markCodPayment(id, deliveryAgentId));
    }

    @RequestMapping(value = {"/{id}/cancel", "/cancel/{id}"}, method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> cancelOrder(
            @PathVariable("id") Long id,
            @RequestParam(value = "customerId", required = false) Long customerId) {
        return ResponseEntity.ok(orderService.cancelOrder(id, customerId));
    }

    @PostMapping({"/{id}/refund", "/refund/{id}"})
    public ResponseEntity<OrderDto> requestRefund(
            @PathVariable("id") Long id,
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(orderService.requestRefund(id, request));
    }

    @GetMapping("/customer/{customerId}/current")
    public ResponseEntity<List<OrderDto>> getCurrentOrdersByCustomerId(@PathVariable("customerId") Long customerId) {
        return ResponseEntity.ok(orderService.getCurrentOrdersByCustomerId(customerId));
    }

    @RequestMapping(value = "/{id}/accept", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> acceptOrder(
            @PathVariable("id") Long id,
            @RequestParam("merchantId") Long merchantId) {
        return ResponseEntity.ok(orderService.acceptOrder(id, merchantId));
    }

    @RequestMapping(value = "/{id}/reject", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> rejectOrder(
            @PathVariable("id") Long id,
            @RequestParam("merchantId") Long merchantId,
            @RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(orderService.rejectOrder(id, merchantId, reason));
    }

    @RequestMapping(value = "/{id}/process", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> processOrder(
            @PathVariable("id") Long id,
            @RequestParam("merchantId") Long merchantId) {
        return ResponseEntity.ok(orderService.processOrder(id, merchantId));
    }

    @RequestMapping(value = "/{id}/pack", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> packOrder(
            @PathVariable("id") Long id,
            @RequestParam("merchantId") Long merchantId) {
        return ResponseEntity.ok(orderService.packOrder(id, merchantId));
    }

    @RequestMapping(value = {"/{id}/ready-for-pickup", "/{id}/ready-pickup", "/{id}/pickup-ready"}, method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> readyForPickup(
            @PathVariable("id") Long id,
            @RequestParam("merchantId") Long merchantId) {
        return ResponseEntity.ok(orderService.readyForPickup(id, merchantId));
    }

    @GetMapping("/merchant/{merchantId}/list")
    public ResponseEntity<List<OrderDto>> getMerchantOrders(
            @PathVariable("merchantId") Long merchantId,
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(orderService.getMerchantOrders(merchantId, status));
    }

    @PostMapping("/returns")
    public ResponseEntity<ReturnRequestDto> createReturnRequest(
            @RequestParam("orderId") Long orderId,
            @RequestParam("customerId") Long customerId,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam("reason") String reason) {
        ReturnRequestDto created = orderService.createReturnRequest(orderId, customerId, productId, quantity, reason);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/returns/merchant/{merchantId}")
    public ResponseEntity<List<ReturnRequestDto>> getMerchantReturnRequests(@PathVariable("merchantId") Long merchantId) {
        return ResponseEntity.ok(orderService.getMerchantReturnRequests(merchantId));
    }

    @GetMapping("/returns/customer/{customerId}")
    public ResponseEntity<List<ReturnRequestDto>> getCustomerReturnRequests(@PathVariable("customerId") Long customerId) {
        return ResponseEntity.ok(orderService.getCustomerReturnRequests(customerId));
    }

    @RequestMapping(value = "/returns/{returnId}/approve", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ReturnRequestDto> approveReturn(
            @PathVariable("returnId") Long returnId,
            @RequestParam("merchantId") Long merchantId) {
        return ResponseEntity.ok(orderService.approveReturn(returnId, merchantId));
    }

    @RequestMapping(value = "/returns/{returnId}/reject", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ReturnRequestDto> rejectReturn(
            @PathVariable("returnId") Long returnId,
            @RequestParam("merchantId") Long merchantId,
            @RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(orderService.rejectReturn(returnId, merchantId, reason));
    }

    @RequestMapping(value = "/returns/{returnId}/receive", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ReturnRequestDto> receiveReturnedProduct(
            @PathVariable("returnId") Long returnId,
            @RequestParam("merchantId") Long merchantId) {
        return ResponseEntity.ok(orderService.receiveReturnedProduct(returnId, merchantId));
    }

    @RequestMapping(value = {"/returns/{returnId}/restock", "/returns/{returnId}/complete"}, method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ReturnRequestDto> restockAndCompleteReturn(
            @PathVariable("returnId") Long returnId,
            @RequestParam("merchantId") Long merchantId) {
        return ResponseEntity.ok(orderService.restockAndCompleteReturn(returnId, merchantId));
    }

    // Delivery Agent Operations (Features 89, 92, 93, 94, 101, 102, 103, 104, 105, 106)
    @RequestMapping(value = "/{id}/accept-assignment", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> acceptAssignment(
            @PathVariable("id") Long id,
            @RequestParam("deliveryAgentId") Long deliveryAgentId) {
        return ResponseEntity.ok(orderService.acceptAssignment(id, deliveryAgentId));
    }

    @RequestMapping(value = "/{id}/reject-assignment", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> rejectAssignment(
            @PathVariable("id") Long id,
            @RequestParam("deliveryAgentId") Long deliveryAgentId,
            @RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(orderService.rejectAssignment(id, deliveryAgentId, reason));
    }

    @RequestMapping(value = "/{id}/pickup", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> pickupPackage(
            @PathVariable("id") Long id,
            @RequestParam("deliveryAgentId") Long deliveryAgentId) {
        return ResponseEntity.ok(orderService.pickupPackage(id, deliveryAgentId));
    }

    @RequestMapping(value = "/{id}/confirm-pickup", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> confirmPickup(
            @PathVariable("id") Long id,
            @RequestParam("deliveryAgentId") Long deliveryAgentId) {
        return ResponseEntity.ok(orderService.confirmPickup(id, deliveryAgentId));
    }

    @RequestMapping(value = "/{id}/delivery-proof", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> uploadDeliveryProof(
            @PathVariable("id") Long id,
            @RequestParam("deliveryAgentId") Long deliveryAgentId,
            @RequestParam("proofUrl") String proofUrl) {
        return ResponseEntity.ok(orderService.uploadDeliveryProof(id, deliveryAgentId, proofUrl));
    }

    @RequestMapping(value = "/{id}/verify-otp", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> verifyDeliveryOtp(
            @PathVariable("id") Long id,
            @RequestParam("deliveryAgentId") Long deliveryAgentId,
            @RequestParam("otp") String otp) {
        return ResponseEntity.ok(orderService.verifyDeliveryOtp(id, deliveryAgentId, otp));
    }

    @RequestMapping(value = "/{id}/failed", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> markDeliveryFailed(
            @PathVariable("id") Long id,
            @RequestParam("deliveryAgentId") Long deliveryAgentId,
            @RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(orderService.markDeliveryFailed(id, deliveryAgentId, reason));
    }

    @RequestMapping(value = "/{id}/reschedule", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> rescheduleDelivery(
            @PathVariable("id") Long id,
            @RequestParam("deliveryAgentId") Long deliveryAgentId,
            @RequestParam(value = "newDate", required = false) java.time.LocalDateTime newDate) {
        return ResponseEntity.ok(orderService.rescheduleDelivery(id, deliveryAgentId, newDate));
    }

    @RequestMapping(value = "/{id}/return-to-merchant", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDto> returnPackageToMerchant(
            @PathVariable("id") Long id,
            @RequestParam("deliveryAgentId") Long deliveryAgentId,
            @RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(orderService.returnPackageToMerchant(id, deliveryAgentId, reason));
    }

    @GetMapping("/search")
    public ResponseEntity<List<OrderDto>> searchOrders(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(orderService.searchOrders(query, status));
    }

    @PostMapping("/{id}/admin-cancel")
    public ResponseEntity<OrderDto> adminCancelOrder(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(orderService.adminCancelOrder(id, reason));
    }

    @PostMapping("/complaints")
    public ResponseEntity<CustomerComplaintDto> createComplaint(@RequestBody CustomerComplaintDto complaintDto) {
        return new ResponseEntity<>(orderService.createComplaint(complaintDto), HttpStatus.CREATED);
    }

    @GetMapping("/complaints")
    public ResponseEntity<List<CustomerComplaintDto>> getAllComplaints() {
        return ResponseEntity.ok(orderService.getAllComplaints());
    }

    @GetMapping("/complaints/{id}")
    public ResponseEntity<CustomerComplaintDto> getComplaintById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.getComplaintById(id));
    }

    @RequestMapping(value = "/complaints/{id}/status", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<CustomerComplaintDto> updateComplaintStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "resolution", required = false) String resolution) {
        return ResponseEntity.ok(orderService.updateComplaintStatus(id, status, resolution));
    }
}
