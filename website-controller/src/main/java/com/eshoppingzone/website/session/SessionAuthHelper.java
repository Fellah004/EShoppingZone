package com.eshoppingzone.website.session;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionAuthHelper {

    public static final String USER_SESSION_KEY = "LOGGED_IN_USER";

    public UserSession getCurrentUser(HttpSession session) {
        if (session == null) return null;
        return (UserSession) session.getAttribute(USER_SESSION_KEY);
    }

    public boolean isAuthenticated(HttpSession session) {
        UserSession user = getCurrentUser(session);
        return user != null && user.isLoggedIn();
    }

    public boolean isMerchant(HttpSession session) {
        UserSession user = getCurrentUser(session);
        return user != null && user.isLoggedIn() && user.isMerchant();
    }

    public boolean isCustomer(HttpSession session) {
        UserSession user = getCurrentUser(session);
        return user != null && user.isLoggedIn() && user.isCustomer();
    }

    public boolean isDeliveryAgent(HttpSession session) {
        UserSession user = getCurrentUser(session);
        return user != null && user.isLoggedIn() && user.isDeliveryAgent();
    }

    public boolean isAdmin(HttpSession session) {
        UserSession user = getCurrentUser(session);
        return user != null && user.isLoggedIn() && user.isAdmin();
    }

    public void setAuthenticatedUser(HttpSession session, UserSession userSession) {
        if (session != null) {
            session.setAttribute(USER_SESSION_KEY, userSession);
        }
    }

    public void clearAuthentication(HttpSession session) {
        if (session != null) {
            session.removeAttribute(USER_SESSION_KEY);
            session.invalidate();
        }
    }
}
