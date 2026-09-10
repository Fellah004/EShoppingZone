package com.eshoppingzone.website.security;

import com.eshoppingzone.website.client.ProfileClient;
import com.eshoppingzone.website.dto.AddressDto;
import com.eshoppingzone.website.dto.AuthResponse;
import com.eshoppingzone.website.dto.RegisterRequest;
import com.eshoppingzone.website.dto.UserProfileDto;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final ProfileClient profileClient;
    private final SessionAuthHelper authHelper;

    public OAuth2LoginSuccessHandler(ProfileClient profileClient, SessionAuthHelper authHelper) {
        this.profileClient = profileClient;
        this.authHelper = authHelper;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String login = oAuth2User.getAttribute("login");

            String fullName = (name != null && !name.isBlank()) ? name : ((login != null && !login.isBlank()) ? login : "GitHub User");
            if (email == null || email.isBlank()) {
                email = (login != null && !login.isBlank() ? login : "github_user") + "@users.noreply.github.com";
            }

            // Check if profile already exists or create new
            UserProfileDto profile = null;
            try {
                profile = profileClient.getProfileByEmail(email);
            } catch (Exception ex) {
                log.debug("Profile lookup for {} returned: {}", email, ex.getMessage());
            }

            if (profile == null) {
                try {
                    RegisterRequest regRequest = new RegisterRequest();
                    regRequest.setFullName(fullName);
                    regRequest.setEmail(email);
                    regRequest.setPassword("OAuth2_GitHub_" + System.currentTimeMillis());
                    long randomPhoneSuffix = (long) (Math.random() * 900000000L + 100000000L);
                    regRequest.setMobileNumber("9" + randomPhoneSuffix);
                    regRequest.setRole("CUSTOMER");
                    regRequest.setAddress(new AddressDto("1", "MG Road", "Downtown", "Kochi", "Kerala", "682030"));
                    AuthResponse authResponse = profileClient.register(regRequest);

                    UserSession userSession = new UserSession(
                            authResponse.getProfileId(),
                            authResponse.getFullName(),
                            authResponse.getEmail(),
                            "CUSTOMER",
                            authResponse.getToken()
                    );
                    authHelper.setAuthenticatedUser(request.getSession(), userSession);
                    log.info("Auto-registered and authenticated new GitHub user: {} (ID: {})", email, authResponse.getProfileId());
                } catch (Exception ex) {
                    log.error("Failed to auto-register GitHub user", ex);
                }
            } else {
                UserSession userSession = new UserSession(
                        profile.getProfileId(),
                        profile.getFullName(),
                        profile.getEmail(),
                        profile.getRole() != null ? profile.getRole() : "CUSTOMER",
                        ""
                );
                authHelper.setAuthenticatedUser(request.getSession(), userSession);
                log.info("Authenticated existing GitHub user: {} (ID: {})", email, profile.getProfileId());
            }
        } catch (Exception ex) {
            log.error("Error in OAuth2 success handler", ex);
        }

        UserSession currentUser = authHelper.getCurrentUser(request.getSession());
        if (currentUser != null) {
            if (currentUser.isAdmin()) {
                response.sendRedirect("/admin/dashboard");
                return;
            } else if (currentUser.isMerchant()) {
                response.sendRedirect("/merchant/dashboard");
                return;
            } else if (currentUser.isDeliveryAgent()) {
                response.sendRedirect("/delivery/dashboard");
                return;
            }
        }

        response.sendRedirect("/");
    }
}
