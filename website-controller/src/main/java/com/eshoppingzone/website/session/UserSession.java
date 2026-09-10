package com.eshoppingzone.website.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession implements Serializable {

    private Long profileId;
    private String fullName;
    private String email;
    private String role;
    private String token;

    public boolean isLoggedIn() {
        return profileId != null;
    }

    public boolean isMerchant() {
        if (role == null) return false;
        return "MERCHANT".equalsIgnoreCase(role) || "ROLE_MERCHANT".equalsIgnoreCase(role);
    }

    public boolean isCustomer() {
        if (role == null) return false;
        return "CUSTOMER".equalsIgnoreCase(role) || "ROLE_CUSTOMER".equalsIgnoreCase(role);
    }

    public boolean isDeliveryAgent() {
        if (role == null) return false;
        return "DELIVERY_AGENT".equalsIgnoreCase(role) || "ROLE_DELIVERY_AGENT".equalsIgnoreCase(role);
    }

    public boolean isAdmin() {
        if (role == null) return false;
        return "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
    }
}
