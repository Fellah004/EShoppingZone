package com.eshoppingzone.website.controller;

import com.eshoppingzone.website.client.CartClient;
import com.eshoppingzone.website.client.ProfileClient;
import com.eshoppingzone.website.dto.AddressDto;
import com.eshoppingzone.website.dto.CartDto;
import com.eshoppingzone.website.dto.UpdateProfileRequest;
import com.eshoppingzone.website.dto.UserProfileDto;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileWebController {

    private final ProfileClient profileClient;
    private final CartClient cartClient;
    private final SessionAuthHelper authHelper;

    public ProfileWebController(ProfileClient profileClient, CartClient cartClient, SessionAuthHelper authHelper) {
        this.profileClient = profileClient;
        this.cartClient = cartClient;
        this.authHelper = authHelper;
    }

    private void populateCartCount(UserSession user, Model model) {
        int cartCount = 0;
        if (user != null && user.isLoggedIn()) {
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
    public String viewProfile(
            @RequestParam(value = "updated", required = false) String updated,
            HttpSession session,
            Model model) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/profile";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);
        populateCartCount(currentUser, model);

        if (updated != null) {
            model.addAttribute("successMessage", "Profile updated successfully!");
        }

        try {
            UserProfileDto profile = profileClient.getProfileById(currentUser.getProfileId());
            model.addAttribute("profile", profile);

            UpdateProfileRequest updateRequest = new UpdateProfileRequest();
            updateRequest.setFullName(profile.getFullName());
            updateRequest.setMobileNumber(profile.getMobileNumber());
            updateRequest.setAbout(profile.getAbout());
            updateRequest.setDateOfBirth(profile.getDateOfBirth());
            updateRequest.setGender(profile.getGender());
            updateRequest.setProfileImage(profile.getProfileImage());
            updateRequest.setAddress(profile.getAddress() != null ? profile.getAddress() : new AddressDto());
            model.addAttribute("updateRequest", updateRequest);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Unable to load profile data.");
        }

        return "profile/profile";
    }

    @PostMapping("/edit")
    public String updateProfile(
            @ModelAttribute UpdateProfileRequest updateRequest,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/profile";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            UserProfileDto updated = profileClient.updateProfile(currentUser.getProfileId(), updateRequest);
            // Update session name if changed
            currentUser.setFullName(updated.getFullName());
            authHelper.setAuthenticatedUser(session, currentUser);

            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile: " + ex.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @ModelAttribute com.eshoppingzone.website.dto.ChangePasswordRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/profile";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Current password is required.");
                return "redirect:/profile";
            }
            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                redirectAttributes.addFlashAttribute("errorMessage", "New password must be at least 6 characters.");
                return "redirect:/profile";
            }
            profileClient.changePassword(currentUser.getProfileId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully! You can now use your new password.");
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("does not match") || msg.contains("match") || msg.contains("InvalidCredentialsException") || msg.contains("401"))) {
                msg = "Current password does not match.";
            } else if (msg != null && msg.contains("least 6 characters")) {
                msg = "Password must be at least 6 characters.";
            } else {
                msg = "Failed to update password. Please verify your current password and try again.";
            }
            redirectAttributes.addFlashAttribute("errorMessage", msg);
        }

        return "redirect:/profile";
    }

    @GetMapping("/addresses")
    public String manageAddresses(HttpSession session, Model model) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/profile/addresses";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);
        model.addAttribute("currentUser", currentUser);
        populateCartCount(currentUser, model);

        try {
            java.util.List<com.eshoppingzone.website.dto.UserAddressDto> addresses = profileClient.getAddresses(currentUser.getProfileId());
            model.addAttribute("addresses", addresses);
        } catch (Exception ex) {
            model.addAttribute("addresses", java.util.Collections.emptyList());
            model.addAttribute("errorMessage", "Could not load saved addresses.");
        }

        model.addAttribute("newAddressRequest", new com.eshoppingzone.website.dto.CreateAddressRequest());
        return "profile/addresses";
    }

    @PostMapping("/addresses")
    public String addAddress(
            @ModelAttribute com.eshoppingzone.website.dto.CreateAddressRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/profile/addresses";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            profileClient.addAddress(currentUser.getProfileId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Address added successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add address: " + ex.getMessage());
        }

        return "redirect:/profile/addresses";
    }

    @PostMapping("/addresses/{id}/edit")
    public String updateAddress(
            @PathVariable("id") Long addressId,
            @ModelAttribute com.eshoppingzone.website.dto.UpdateAddressRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/profile/addresses";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            profileClient.updateAddress(currentUser.getProfileId(), addressId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Address updated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update address: " + ex.getMessage());
        }

        return "redirect:/profile/addresses";
    }

    @PostMapping("/addresses/{id}/delete")
    public String deleteAddress(
            @PathVariable("id") Long addressId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/profile/addresses";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            profileClient.deleteAddress(currentUser.getProfileId(), addressId);
            redirectAttributes.addFlashAttribute("successMessage", "Address deleted successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete address: " + ex.getMessage());
        }

        return "redirect:/profile/addresses";
    }

    @PostMapping("/addresses/{id}/default")
    public String setDefaultAddress(
            @PathVariable("id") Long addressId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login?redirect=/profile/addresses";
        }
        UserSession currentUser = authHelper.getCurrentUser(session);

        try {
            profileClient.setDefaultAddress(currentUser.getProfileId(), addressId);
            redirectAttributes.addFlashAttribute("successMessage", "Default address updated!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to set default address: " + ex.getMessage());
        }

        return "redirect:/profile/addresses";
    }
}
