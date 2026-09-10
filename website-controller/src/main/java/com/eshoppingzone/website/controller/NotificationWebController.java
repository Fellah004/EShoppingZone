package com.eshoppingzone.website.controller;

import com.eshoppingzone.website.client.CartClient;
import com.eshoppingzone.website.client.NotificationClient;
import com.eshoppingzone.website.dto.CartDto;
import com.eshoppingzone.website.dto.NotificationDto;
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
@RequestMapping("/notifications")
public class NotificationWebController {

    private final NotificationClient notificationClient;
    private final CartClient cartClient;
    private final SessionAuthHelper authHelper;

    public NotificationWebController(NotificationClient notificationClient, CartClient cartClient, SessionAuthHelper authHelper) {
        this.notificationClient = notificationClient;
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

    private long getUnreadCount(Long profileId) {
        if (profileId == null) return 0;
        try {
            var res = notificationClient.getUnreadCount(profileId);
            if (res != null && res.containsKey("unreadCount")) {
                Object val = res.get("unreadCount");
                if (val instanceof Number n) {
                    return n.longValue();
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    @GetMapping
    public String viewNotifications(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/notifications";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);
        populateCartCount(currentUser, model);

        List<NotificationDto> allNotifications = new ArrayList<>();
        long unreadCount = 0;
        try {
            allNotifications = notificationClient.getNotificationsByRecipient(currentUser.getProfileId());
            unreadCount = getUnreadCount(currentUser.getProfileId());
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Unable to load notifications.");
        }

        com.eshoppingzone.website.dto.PagedResult<NotificationDto> pageData = com.eshoppingzone.website.dto.PagedResult.from(allNotifications, page, size);

        model.addAttribute("notifications", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("baseUrl", "/notifications");
        model.addAttribute("unreadCount", unreadCount);
        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            notificationClient.markAsRead(id, currentUser.getProfileId());
        } catch (Exception ignored) {
        }
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        try {
            notificationClient.markAllAsRead(currentUser.getProfileId());
            redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to mark notifications as read.");
        }
        return "redirect:/notifications";
    }
}
