package com.eshoppingzone.website.controller;

import com.eshoppingzone.website.client.OrderClient;
import com.eshoppingzone.website.client.ProfileClient;
import com.eshoppingzone.website.dto.OrderDto;
import com.eshoppingzone.website.dto.UpdateDeliveryStatusRequest;
import com.eshoppingzone.website.dto.UpdateProfileRequest;
import com.eshoppingzone.website.dto.UserProfileDto;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/delivery")
public class DeliveryAgentWebController {

    private static final Logger log = LoggerFactory.getLogger(DeliveryAgentWebController.class);

    private final OrderClient orderClient;
    private final ProfileClient profileClient;
    private final SessionAuthHelper authHelper;

    public DeliveryAgentWebController(OrderClient orderClient, ProfileClient profileClient, SessionAuthHelper authHelper) {
        this.orderClient = orderClient;
        this.profileClient = profileClient;
        this.authHelper = authHelper;
    }

    private boolean checkDeliveryAgent(HttpSession session) {
        return authHelper.isDeliveryAgent(session);
    }

    @GetMapping({"", "/", "/dashboard"})
    public String deliveryDashboard(HttpSession session, Model model) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/dashboard";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        UserProfileDto profile = null;
        try {
            profile = profileClient.getProfileById(currentUser.getProfileId());
            model.addAttribute("profile", profile);
        } catch (Exception ex) {
            log.warn("Failed to fetch agent profile: {}", ex.getMessage());
        }

        List<OrderDto> assignedOrders = new ArrayList<>();
        try {
            assignedOrders = orderClient.getOrdersByDeliveryAgentId(currentUser.getProfileId());
        } catch (Exception ex) {
            log.warn("Failed to fetch delivery agent orders: {}", ex.getMessage());
            model.addAttribute("errorMessage", "Unable to load assigned orders.");
        }

        long assignedCount = assignedOrders.stream().filter(o -> "ASSIGNED".equalsIgnoreCase(o.getOrderStatus())).count();
        long outForDeliveryCount = assignedOrders.stream().filter(o -> "OUT_FOR_DELIVERY".equalsIgnoreCase(o.getOrderStatus())).count();
        long deliveredCount = assignedOrders.stream().filter(o -> "DELIVERED".equalsIgnoreCase(o.getOrderStatus())).count();

        model.addAttribute("assignedCount", assignedCount);
        model.addAttribute("outForDeliveryCount", outForDeliveryCount);
        model.addAttribute("deliveredCount", deliveredCount);
        model.addAttribute("totalAssigned", assignedOrders.size());
        model.addAttribute("recentOrders", assignedOrders.stream().limit(5).collect(Collectors.toList()));

        return "delivery/dashboard";
    }

    @GetMapping("/orders")
    public String assignedOrdersList(
            @RequestParam(value = "status", required = false) String statusFilter,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        List<OrderDto> assignedOrders = new ArrayList<>();
        try {
            assignedOrders = orderClient.getOrdersByDeliveryAgentId(currentUser.getProfileId());
        } catch (Exception ex) {
            log.warn("Failed to fetch assigned orders: {}", ex.getMessage());
            model.addAttribute("errorMessage", "Unable to load assigned orders.");
        }

        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            assignedOrders = assignedOrders.stream()
                    .filter(o -> statusFilter.equalsIgnoreCase(o.getOrderStatus()))
                    .collect(Collectors.toList());
        }

        com.eshoppingzone.website.dto.PagedResult<OrderDto> pageData = com.eshoppingzone.website.dto.PagedResult.from(assignedOrders, page, size);
        String baseUrl = "/delivery/orders" + (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter) ? "?status=" + java.net.URLEncoder.encode(statusFilter, java.nio.charset.StandardCharsets.UTF_8) : "");

        model.addAttribute("orders", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("currentFilter", statusFilter != null ? statusFilter : "ALL");
        return "delivery/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders/" + id;
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        try {
            OrderDto order = orderClient.getOrderById(id);
            if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(currentUser.getProfileId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You are not assigned to Order #" + id);
                return "redirect:/delivery/orders";
            }
            model.addAttribute("order", order);

            // 90 & 91. Merchant Pickup Location & Navigation
            UserProfileDto merchant = null;
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                Long merchantId = order.getItems().get(0).getMerchantId();
                if (merchantId != null) {
                    try {
                        merchant = profileClient.getProfileById(merchantId);
                    } catch (Exception ex) {
                        log.warn("Could not load merchant profile #{}: {}", merchantId, ex.getMessage());
                    }
                }
            }
            model.addAttribute("merchant", merchant);

            return "delivery/order-detail";
        } catch (Exception ex) {
            log.error("Failed to load order #{}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Order not found.");
            return "redirect:/delivery/orders";
        }
    }

    // 89. Accept Assignment
    @PostMapping("/orders/{id}/accept")
    public String acceptAssignment(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders/" + id;
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            orderClient.acceptAssignment(id, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Assignment accepted! Proceed to merchant location for pickup.");
        } catch (Exception ex) {
            log.error("Failed to accept assignment: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to accept assignment: " + ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }

    // 89. Reject Assignment
    @PostMapping("/orders/{id}/reject")
    public String rejectAssignment(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            orderClient.rejectAssignment(id, currentUser.getProfileId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment rejected. Order returned to pool for reassignment.");
        } catch (Exception ex) {
            log.error("Failed to reject assignment: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject assignment: " + ex.getMessage());
        }
        return "redirect:/delivery/orders";
    }

    // 92. Pick Up Package
    @PostMapping("/orders/{id}/pickup")
    public String pickupPackage(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders/" + id;
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            orderClient.pickupPackage(id, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Package marked as PICKED UP from merchant!");
        } catch (Exception ex) {
            log.error("Failed to mark pickup: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to mark pickup: " + ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }

    // 93 & 94. Confirm Pickup & Update Status (Out for Delivery)
    @PostMapping("/orders/{id}/confirm-pickup")
    public String confirmPickup(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders/" + id;
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            orderClient.confirmPickup(id, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Pickup confirmed! Order is now OUT FOR DELIVERY. Customer has received their 6-digit delivery OTP.");
        } catch (Exception ex) {
            log.error("Failed to confirm pickup: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to confirm pickup: " + ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }

    // 101. Upload Delivery Proof
    @PostMapping("/orders/{id}/proof")
    public String uploadDeliveryProof(
            @PathVariable("id") Long id,
            @RequestParam("proofUrl") String proofUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders/" + id;
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            orderClient.uploadDeliveryProof(id, currentUser.getProfileId(), proofUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Delivery proof document/photo uploaded successfully!");
        } catch (Exception ex) {
            log.error("Failed to upload delivery proof: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload delivery proof: " + ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }

    // 102. Verify Delivery OTP
    @PostMapping("/orders/{id}/verify-otp")
    public String verifyDeliveryOtp(
            @PathVariable("id") Long id,
            @RequestParam("otp") String otp,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders/" + id;
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            orderClient.verifyDeliveryOtp(id, currentUser.getProfileId(), otp);
            redirectAttributes.addFlashAttribute("successMessage", "OTP verified! Order #" + id + " marked as DELIVERED successfully.");
        } catch (Exception ex) {
            log.error("Failed to verify delivery OTP: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "OTP Verification Failed: " + ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }

    // 103 & 104. Mark Delivery Failed & Enter Failure Reason
    @PostMapping("/orders/{id}/failed")
    public String markDeliveryFailed(
            @PathVariable("id") Long id,
            @RequestParam("reason") String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders/" + id;
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            orderClient.markDeliveryFailed(id, currentUser.getProfileId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Delivery attempt marked as failed: " + reason);
        } catch (Exception ex) {
            log.error("Failed to mark delivery failed: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to record delivery failure: " + ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }

    // 105. Reschedule Delivery
    @PostMapping("/orders/{id}/reschedule")
    public String rescheduleDelivery(
            @PathVariable("id") Long id,
            @RequestParam(value = "newDate", required = false) String newDateStr,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders/" + id;
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            orderClient.rescheduleDelivery(id, currentUser.getProfileId(), newDateStr);
            redirectAttributes.addFlashAttribute("successMessage", "Delivery rescheduled successfully.");
        } catch (Exception ex) {
            log.error("Failed to reschedule delivery: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reschedule delivery: " + ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }

    // 106. Return Package to Merchant
    @PostMapping("/orders/{id}/return")
    public String returnPackageToMerchant(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders/" + id;
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            orderClient.returnPackageToMerchant(id, currentUser.getProfileId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Package returned to merchant. Order cancelled and customer refunded.");
        } catch (Exception ex) {
            log.error("Failed to return package to merchant: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to return package: " + ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }

    @PostMapping("/orders/{id}/update-status")
    public String updateDeliveryStatus(
            @PathVariable("id") Long id,
            @RequestParam("newStatus") String newStatus,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders/" + id;
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            UpdateDeliveryStatusRequest req = new UpdateDeliveryStatusRequest(currentUser.getProfileId(), newStatus);
            orderClient.updateDeliveryStatus(id, req);
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " updated to " + newStatus + ".");
        } catch (Exception ex) {
            log.error("Failed to update delivery status: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update status: " + ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }

    @PostMapping("/orders/{id}/mark-cod-paid")
    public String markCodPaid(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/orders/" + id;
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            orderClient.markCodPayment(id, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " COD payment marked as PAID.");
        } catch (Exception ex) {
            log.error("Failed to mark COD payment: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update COD payment: " + ex.getMessage());
        }
        return "redirect:/delivery/orders/" + id;
    }

    // 108, 109, 110: Daily Earnings, Incentives & Payment History
    @GetMapping("/earnings")
    public String earningsView(
            @RequestParam(value = "date", required = false) String dateStr,
            HttpSession session,
            Model model) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/earnings";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        java.time.LocalDate targetDate = (dateStr != null && !dateStr.isBlank())
                ? java.time.LocalDate.parse(dateStr)
                : java.time.LocalDate.now();

        List<OrderDto> allAgentOrders = new ArrayList<>();
        try {
            allAgentOrders = orderClient.getOrdersByDeliveryAgentId(currentUser.getProfileId());
        } catch (Exception ex) {
            log.warn("Failed to fetch orders for earnings: {}", ex.getMessage());
        }

        List<OrderDto> completedOrders = allAgentOrders.stream()
                .filter(o -> "DELIVERED".equalsIgnoreCase(o.getOrderStatus()))
                .collect(Collectors.toList());

        List<OrderDto> dailyDeliveredOrders = completedOrders.stream()
                .filter(o -> {
                    if (o.getDeliveredDate() != null) {
                        return o.getDeliveredDate().toLocalDate().equals(targetDate);
                    }
                    if (o.getOrderDate() != null) {
                        return o.getOrderDate().toLocalDate().equals(targetDate);
                    }
                    return false;
                })
                .collect(Collectors.toList());

        long dailyCount = dailyDeliveredOrders.size();
        double tripFee = 50.0;
        double dailyEarnings = dailyCount * tripFee;

        // 109. Milestone Incentives
        double dailyIncentives = 0.0;
        String incentiveTier = "Base (No Bonus)";
        if (dailyCount >= 20) {
            dailyIncentives = 600.0;
            incentiveTier = "Tier 3 Super Bonus (+₹600)";
        } else if (dailyCount >= 10) {
            dailyIncentives = 250.0;
            incentiveTier = "Tier 2 Bonus (+₹250)";
        } else if (dailyCount >= 5) {
            dailyIncentives = 100.0;
            incentiveTier = "Tier 1 Bonus (+₹100)";
        }

        long totalDeliveredCount = completedOrders.size();
        double totalEarnings = totalDeliveredCount * tripFee;

        model.addAttribute("selectedDate", targetDate.toString());
        model.addAttribute("dailyCount", dailyCount);
        model.addAttribute("dailyEarnings", dailyEarnings);
        model.addAttribute("dailyIncentives", dailyIncentives);
        model.addAttribute("incentiveTier", incentiveTier);
        model.addAttribute("totalDeliveredCount", totalDeliveredCount);
        model.addAttribute("totalEarnings", totalEarnings);
        model.addAttribute("completedOrders", completedOrders);

        return "delivery/earnings";
    }

    // Features 83, 84, 85, 86, 87: Delivery Agent Profile, Vehicle & Availability
    @GetMapping("/profile")
    public String agentProfile(HttpSession session, Model model) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/profile";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        try {
            UserProfileDto profile = profileClient.getProfileById(currentUser.getProfileId());
            model.addAttribute("profile", profile);

            UpdateProfileRequest updateRequest = new UpdateProfileRequest();
            updateRequest.setFullName(profile.getFullName());
            updateRequest.setMobileNumber(profile.getMobileNumber());
            updateRequest.setGender(profile.getGender());
            updateRequest.setDateOfBirth(profile.getDateOfBirth());
            updateRequest.setProfileImage(profile.getProfileImage());
            updateRequest.setVehicleType(profile.getVehicleType());
            updateRequest.setVehicleNumber(profile.getVehicleNumber());
            updateRequest.setVehicleModel(profile.getVehicleModel());
            updateRequest.setDrivingLicenseNumber(profile.getDrivingLicenseNumber());
            updateRequest.setDrivingLicenseUrl(profile.getDrivingLicenseUrl());
            updateRequest.setVehicleRegistrationUrl(profile.getVehicleRegistrationUrl());
            updateRequest.setIdentityDocumentUrl(profile.getIdentityDocumentUrl());
            updateRequest.setAvailability(profile.getAvailability());
            updateRequest.setAddress(profile.getAddress() != null ? profile.getAddress() : new com.eshoppingzone.website.dto.AddressDto());

            model.addAttribute("updateRequest", updateRequest);
        } catch (Exception ex) {
            log.error("Failed to load delivery agent profile: {}", ex.getMessage());
            model.addAttribute("errorMessage", "Failed to load agent profile.");
        }

        return "delivery/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @ModelAttribute UpdateProfileRequest updateRequest,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/profile";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            UserProfileDto updated = profileClient.updateProfile(currentUser.getProfileId(), updateRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Profile & vehicle details updated successfully!");
        } catch (Exception ex) {
            log.error("Failed to update agent profile: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile: " + ex.getMessage());
        }
        return "redirect:/delivery/profile";
    }

    @PostMapping("/documents")
    public String updateDocuments(
            @RequestParam(value = "drivingLicenseUrl", required = false) String drivingLicenseUrl,
            @RequestParam(value = "vehicleRegistrationUrl", required = false) String vehicleRegistrationUrl,
            @RequestParam(value = "identityDocumentUrl", required = false) String identityDocumentUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/profile";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            UserProfileDto profile = profileClient.getProfileById(currentUser.getProfileId());
            UpdateProfileRequest updateRequest = new UpdateProfileRequest();
            updateRequest.setFullName(profile.getFullName());
            updateRequest.setMobileNumber(profile.getMobileNumber());
            updateRequest.setGender(profile.getGender());
            updateRequest.setDateOfBirth(profile.getDateOfBirth());
            updateRequest.setProfileImage(profile.getProfileImage());
            updateRequest.setVehicleType(profile.getVehicleType());
            updateRequest.setVehicleNumber(profile.getVehicleNumber());
            updateRequest.setVehicleModel(profile.getVehicleModel());
            updateRequest.setDrivingLicenseNumber(profile.getDrivingLicenseNumber());
            updateRequest.setDrivingLicenseUrl(drivingLicenseUrl);
            updateRequest.setVehicleRegistrationUrl(vehicleRegistrationUrl);
            updateRequest.setIdentityDocumentUrl(identityDocumentUrl);
            updateRequest.setAvailability(profile.getAvailability());
            updateRequest.setAddress(profile.getAddress() != null ? profile.getAddress() : new com.eshoppingzone.website.dto.AddressDto());

            profileClient.updateProfile(currentUser.getProfileId(), updateRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Verification documents updated successfully!");
        } catch (Exception ex) {
            log.error("Failed to update documents: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update documents: " + ex.getMessage());
        }
        return "redirect:/delivery/profile";
    }

    @PostMapping("/availability")
    public String updateAvailability(
            @RequestParam("status") String status,
            @RequestParam(value = "redirect", required = false) String redirectUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!checkDeliveryAgent(session)) {
            return "redirect:/login?redirect=/delivery/dashboard";
        }

        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            profileClient.updateAvailability(currentUser.getProfileId(), status);
            redirectAttributes.addFlashAttribute("successMessage", "Duty status updated to: " + status);
        } catch (Exception ex) {
            log.error("Failed to update availability: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update status: " + ex.getMessage());
        }
        return redirectUrl != null && !redirectUrl.isBlank() ? "redirect:" + redirectUrl : "redirect:/delivery/dashboard";
    }
}
