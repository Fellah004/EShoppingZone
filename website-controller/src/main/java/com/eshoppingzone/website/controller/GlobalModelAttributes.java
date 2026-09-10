package com.eshoppingzone.website.controller;

import com.eshoppingzone.website.client.CartClient;
import com.eshoppingzone.website.client.NotificationClient;
import com.eshoppingzone.website.dto.CartDto;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final SessionAuthHelper authHelper;
    private final NotificationClient notificationClient;
    private final CartClient cartClient;

    public GlobalModelAttributes(
            SessionAuthHelper authHelper,
            NotificationClient notificationClient,
            CartClient cartClient) {
        this.authHelper = authHelper;
        this.notificationClient = notificationClient;
        this.cartClient = cartClient;
    }

    @ModelAttribute("currentUser")
    public UserSession currentUser(HttpSession session) {
        return authHelper.getCurrentUser(session);
    }

    @ModelAttribute("unreadNotificationCount")
    public Long unreadNotificationCount(HttpSession session) {
        UserSession user = authHelper.getCurrentUser(session);
        if (user != null && user.isLoggedIn()) {
            try {
                java.util.Map<String, Object> countMap = notificationClient.getUnreadCount(user.getProfileId());
                if (countMap != null && countMap.containsKey("unreadCount")) {
                    Object val = countMap.get("unreadCount");
                    if (val instanceof Number n) {
                        return n.longValue();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return 0L;
    }

    @ModelAttribute("cartCount")
    public Integer cartCount(HttpSession session) {
        UserSession user = authHelper.getCurrentUser(session);
        if (user != null && user.isLoggedIn() && user.isCustomer()) {
            try {
                CartDto cart = cartClient.getCartByCustomerId(user.getProfileId());
                if (cart != null) {
                    return cart.getTotalItemCount();
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }
}
