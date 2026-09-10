package com.eshoppingzone.website.controller;

import com.eshoppingzone.website.client.ProfileClient;
import com.eshoppingzone.website.dto.AuthResponse;
import com.eshoppingzone.website.dto.LoginRequest;
import com.eshoppingzone.website.dto.RegisterRequest;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import feign.FeignException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthWebController {

    private final ProfileClient profileClient;
    private final SessionAuthHelper authHelper;

    public AuthWebController(ProfileClient profileClient, SessionAuthHelper authHelper) {
        this.profileClient = profileClient;
        this.authHelper = authHelper;
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpSession session,
            Model model) {
        if (authHelper.isAuthenticated(session)) {
            UserSession user = authHelper.getCurrentUser(session);
            if (user.isAdmin()) {
                return "redirect:/admin/dashboard";
            }
            if (user.isDeliveryAgent()) {
                return "redirect:/delivery/dashboard";
            }
            if (user.isMerchant()) {
                return "redirect:/merchant/dashboard";
            }
            return "redirect:/";
        }
        if (logout != null) {
            model.addAttribute("infoMessage", "You have been logged out successfully.");
        }
        if (error != null) {
            model.addAttribute("errorMessage", "Authentication could not be completed. Note: Live GitHub OAuth requires valid GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET credentials configured.");
        }
        model.addAttribute("loginRequest", new LoginRequest());
        model.addAttribute("redirect", redirect);
        return "auth/login";
    }

    @GetMapping("/oauth2/authorization/github")
    public String githubAuthRedirect(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Live GitHub OAuth requires valid GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET environment variables configured.");
        return "redirect:/login?error=missing_github_credentials";
    }

    @PostMapping("/login")
    public String handleLogin(
            @ModelAttribute LoginRequest loginRequest,
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            AuthResponse response = profileClient.login(loginRequest);
            UserSession userSession = new UserSession(
                    response.getProfileId(),
                    response.getFullName(),
                    response.getEmail(),
                    response.getRole() != null ? response.getRole() : "CUSTOMER",
                    response.getToken()
            );
            authHelper.setAuthenticatedUser(session, userSession);
            redirectAttributes.addFlashAttribute("successMessage", "Welcome back, " + response.getFullName() + "!");

            if (userSession.isAdmin()) {
                return "redirect:/admin/dashboard";
            }

            if (userSession.isDeliveryAgent()) {
                return "redirect:/delivery/dashboard";
            }

            if (userSession.isMerchant()) {
                return "redirect:/merchant/dashboard";
            }

            if (redirect != null && !redirect.isBlank() && !redirect.contains("login") && !redirect.contains("register") && !redirect.contains("merchant") && !redirect.contains("delivery") && !redirect.contains("admin")) {
                return "redirect:" + redirect;
            }
            return "redirect:/";
        } catch (FeignException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid email or password. Please try again.");
            return "redirect:/login" + (redirect != null ? "?redirect=" + redirect : "");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication service error: " + ex.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(
            @RequestParam(value = "role", required = false) String role,
            HttpSession session,
            Model model) {
        if (authHelper.isAuthenticated(session)) {
            UserSession user = authHelper.getCurrentUser(session);
            if (user.isMerchant()) {
                return "redirect:/merchant/dashboard";
            }
            return "redirect:/";
        }
        RegisterRequest request = new RegisterRequest();
        if (role != null && !role.isBlank()) {
            request.setRole(role.toUpperCase());
        }
        model.addAttribute("registerRequest", request);
        model.addAttribute("selectedRole", role != null ? role.toUpperCase() : "CUSTOMER");
        return "auth/register";
    }

    @GetMapping("/merchant/register")
    public String merchantRegisterPage(HttpSession session, Model model) {
        return registerPage("MERCHANT", session, model);
    }

    @GetMapping("/delivery/register")
    public String deliveryRegisterPage(HttpSession session, Model model) {
        return registerPage("DELIVERY_AGENT", session, model);
    }

    @PostMapping("/register")
    public String handleRegister(
            @ModelAttribute RegisterRequest registerRequest,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            AuthResponse response = profileClient.register(registerRequest);
            UserSession userSession = new UserSession(
                    response.getProfileId(),
                    response.getFullName(),
                    response.getEmail(),
                    response.getRole() != null ? response.getRole() : "CUSTOMER",
                    response.getToken()
            );
            authHelper.setAuthenticatedUser(session, userSession);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully! Welcome to EShoppingZone, " + response.getFullName() + "!");

            if (userSession.isAdmin()) {
                return "redirect:/admin/dashboard";
            }
            if (userSession.isDeliveryAgent()) {
                return "redirect:/delivery/dashboard";
            }
            if (userSession.isMerchant()) {
                return "redirect:/merchant/dashboard";
            }
            return "redirect:/";
        } catch (FeignException ex) {
            if (ex.status() == 409 || ex.getMessage().contains("already exists")) {
                redirectAttributes.addFlashAttribute("errorMessage", "An account with this email or mobile number already exists.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Registration failed. Please verify your details.");
            }
            return "redirect:/register";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registration error: " + ex.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {
        authHelper.clearAuthentication(session);
        return "redirect:/login?logout=true";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordRequest", new com.eshoppingzone.website.dto.ForgotPasswordRequest());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @ModelAttribute com.eshoppingzone.website.dto.ForgotPasswordRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            java.util.Map<String, String> response = profileClient.forgotPassword(request);
            String token = response.get("token");
            redirectAttributes.addFlashAttribute("successMessage", "Password reset instructions and token have been generated.");
            redirectAttributes.addFlashAttribute("resetToken", token);
            return "redirect:/reset-password?token=" + (token != null ? token : "");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error requesting password reset: " + ex.getMessage());
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token, Model model) {
        com.eshoppingzone.website.dto.ResetPasswordRequest request = new com.eshoppingzone.website.dto.ResetPasswordRequest();
        if (token != null) {
            request.setToken(token);
        }
        model.addAttribute("resetPasswordRequest", request);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(
            @ModelAttribute com.eshoppingzone.website.dto.ResetPasswordRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            profileClient.resetPassword(request);
            redirectAttributes.addFlashAttribute("successMessage", "Password has been successfully reset! You can now log in with your new password.");
            return "redirect:/login";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reset password: " + ex.getMessage());
            return "redirect:/reset-password" + (request.getToken() != null ? "?token=" + request.getToken() : "");
        }
    }
}
