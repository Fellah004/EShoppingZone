package com.eshoppingzone.website.controller;

import com.eshoppingzone.website.client.CartClient;
import com.eshoppingzone.website.client.OrderClient;
import com.eshoppingzone.website.dto.CartDto;
import com.eshoppingzone.website.dto.OrderDto;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderWebController {

    private final OrderClient orderClient;
    private final CartClient cartClient;
    private final SessionAuthHelper authHelper;

    public OrderWebController(OrderClient orderClient, CartClient cartClient, SessionAuthHelper authHelper) {
        this.orderClient = orderClient;
        this.cartClient = cartClient;
        this.authHelper = authHelper;
    }

    private void populateCartCount(UserSession user, Model model) {
        int cartCount = 0;
        if (user != null && user.isLoggedIn() && user.isCustomer()) {
            try {
                CartDto cart = cartClient.getCartByCustomerId(user.getProfileId());
                if (cart != null) {
                    cartCount = cart.getTotalItemCount();
                }
            } catch (Exception ignored) {
            }
        }
        model.addAttribute("cartCount", cartCount);
    }

    @GetMapping
    public String orderHistory(
            @RequestParam(value = "view", required = false, defaultValue = "all") String view,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/orders";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        if (currentUser.isMerchant()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants do not have a customer order history.");
            return "redirect:/merchant/dashboard";
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentView", view);
        populateCartCount(currentUser, model);

        List<OrderDto> allOrders = new ArrayList<>();
        try {
            if ("current".equalsIgnoreCase(view)) {
                allOrders = orderClient.getCurrentOrdersByCustomerId(currentUser.getProfileId());
            } else {
                allOrders = orderClient.getOrdersByCustomerId(currentUser.getProfileId());
            }
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Unable to retrieve order history.");
        }

        com.eshoppingzone.website.dto.PagedResult<OrderDto> pageData = com.eshoppingzone.website.dto.PagedResult.from(allOrders, page, size);
        model.addAttribute("orders", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("baseUrl", "/orders" + ("current".equalsIgnoreCase(view) ? "?view=current" : ""));
        return "orders/history";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            orderClient.cancelOrder(id, currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " has been successfully cancelled.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to cancel order: " + ex.getMessage());
        }

        return "redirect:/orders";
    }

    @PostMapping("/{id}/refund")
    public String requestRefund(
            @PathVariable("id") Long id,
            @RequestParam("reason") String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            com.eshoppingzone.website.dto.RefundRequest request = new com.eshoppingzone.website.dto.RefundRequest(
                    currentUser.getProfileId(),
                    reason
            );
            orderClient.requestRefund(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Refund request for Order #" + id + " has been submitted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to request refund: " + ex.getMessage());
        }

        return "redirect:/orders";
    }

    @GetMapping("/{id}/invoice")
    public String viewInvoice(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/orders/" + id + "/invoice";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);

        try {
            OrderDto order = orderClient.getOrderById(id);
            if (!currentUser.isAdmin() && !order.getCustomerId().equals(currentUser.getProfileId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission to view this invoice.");
                return "redirect:/orders";
            }
            model.addAttribute("order", order);
            return "orders/invoice";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to load invoice: " + ex.getMessage());
            return "redirect:/orders";
        }
    }

    @GetMapping("/{id}/confirmation")
    public String orderConfirmation(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        if (currentUser.isMerchant()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Merchants do not have customer orders.");
            return "redirect:/merchant/dashboard";
        }

        model.addAttribute("currentUser", currentUser);
        populateCartCount(currentUser, model);

        try {
            OrderDto order = orderClient.getOrderById(id);
            if (!currentUser.isAdmin() && !currentUser.isDeliveryAgent() && !order.getCustomerId().equals(currentUser.getProfileId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not own Order #" + id);
                return "redirect:/orders";
            }
            model.addAttribute("order", order);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Unable to load order details.");
            return "redirect:/orders";
        }

        return "orders/confirmation";
    }

    @GetMapping("/{id}")
    public String orderDetails(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        return orderConfirmation(id, session, model, redirectAttributes);
    }

    @GetMapping("/{id}/track")
    public String trackOrder(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/orders/" + id + "/track";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);
        populateCartCount(currentUser, model);

        try {
            OrderDto order = orderClient.getOrderById(id);
            if (!currentUser.isAdmin() && !currentUser.isDeliveryAgent() && !currentUser.isMerchant() && !order.getCustomerId().equals(currentUser.getProfileId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Access denied: You do not have permission to view Order #" + id);
                return "redirect:/orders";
            }
            model.addAttribute("order", order);
            return "orders/track";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to load tracking details for Order #" + id);
            if (currentUser.isAdmin()) {
                return "redirect:/admin/assignments";
            }
            if (currentUser.isDeliveryAgent()) {
                return "redirect:/delivery/orders";
            }
            if (currentUser.isMerchant()) {
                return "redirect:/merchant/sales";
            }
            return "redirect:/orders";
        }
    }
}
