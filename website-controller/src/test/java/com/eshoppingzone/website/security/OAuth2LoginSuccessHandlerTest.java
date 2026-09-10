package com.eshoppingzone.website.security;

import com.eshoppingzone.website.client.ProfileClient;
import com.eshoppingzone.website.dto.AuthResponse;
import com.eshoppingzone.website.dto.RegisterRequest;
import com.eshoppingzone.website.dto.UserProfileDto;
import com.eshoppingzone.website.session.SessionAuthHelper;
import com.eshoppingzone.website.session.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private ProfileClient profileClient;

    @Mock
    private SessionAuthHelper authHelper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    private OAuth2LoginSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2LoginSuccessHandler(profileClient, authHelper);
        when(request.getSession()).thenReturn(session);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
    }

    @Test
    void testNewGitHubUser_AutoRegistersAsCustomerAndSetsSession() throws Exception {
        when(oAuth2User.getAttribute("email")).thenReturn("octocat@github.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Monalisa Octocat");
        when(oAuth2User.getAttribute("login")).thenReturn("octocat");

        when(profileClient.getProfileByEmail("octocat@github.com")).thenThrow(new RuntimeException("Not found"));

        AuthResponse authResponse = AuthResponse.builder()
                .token("mock-jwt-token")
                .profileId(101L)
                .email("octocat@github.com")
                .fullName("Monalisa Octocat")
                .role("CUSTOMER")
                .build();
        when(profileClient.register(any(RegisterRequest.class))).thenReturn(authResponse);

        UserSession userSession = new UserSession(101L, "Monalisa Octocat", "octocat@github.com", "CUSTOMER", "mock-jwt-token");
        when(authHelper.getCurrentUser(session)).thenReturn(userSession);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<RegisterRequest> regCaptor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(profileClient).register(regCaptor.capture());
        RegisterRequest captured = regCaptor.getValue();

        assertEquals("Monalisa Octocat", captured.getFullName());
        assertEquals("octocat@github.com", captured.getEmail());
        assertEquals("CUSTOMER", captured.getRole());

        verify(authHelper).setAuthenticatedUser(eq(session), any(UserSession.class));
        verify(response).sendRedirect("/");
    }

    @Test
    void testExistingGitHubUser_ReusesProfileWithoutDuplicateRegistration() throws Exception {
        when(oAuth2User.getAttribute("email")).thenReturn("existing_dev@github.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Existing Dev");
        when(oAuth2User.getAttribute("login")).thenReturn("existingdev");

        UserProfileDto existingProfile = new UserProfileDto();
        existingProfile.setProfileId(202L);
        existingProfile.setFullName("Existing Dev");
        existingProfile.setEmail("existing_dev@github.com");
        existingProfile.setRole("CUSTOMER");

        when(profileClient.getProfileByEmail("existing_dev@github.com")).thenReturn(existingProfile);

        UserSession userSession = new UserSession(202L, "Existing Dev", "existing_dev@github.com", "CUSTOMER", "");
        when(authHelper.getCurrentUser(session)).thenReturn(userSession);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(profileClient, never()).register(any(RegisterRequest.class));
        verify(authHelper).setAuthenticatedUser(eq(session), any(UserSession.class));
        verify(response).sendRedirect("/");
    }

    @Test
    void testPrivateEmailGitHubUser_UsesNoreplyFallback() throws Exception {
        when(oAuth2User.getAttribute("email")).thenReturn(null);
        when(oAuth2User.getAttribute("name")).thenReturn("Private User");
        when(oAuth2User.getAttribute("login")).thenReturn("privateuser");

        String expectedFallbackEmail = "privateuser@users.noreply.github.com";
        when(profileClient.getProfileByEmail(expectedFallbackEmail)).thenThrow(new RuntimeException("Not found"));

        AuthResponse authResponse = AuthResponse.builder()
                .token("token-123")
                .profileId(303L)
                .email(expectedFallbackEmail)
                .fullName("Private User")
                .role("CUSTOMER")
                .build();
        when(profileClient.register(any(RegisterRequest.class))).thenReturn(authResponse);

        UserSession userSession = new UserSession(303L, "Private User", expectedFallbackEmail, "CUSTOMER", "token-123");
        when(authHelper.getCurrentUser(session)).thenReturn(userSession);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<RegisterRequest> regCaptor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(profileClient).register(regCaptor.capture());
        assertEquals(expectedFallbackEmail, regCaptor.getValue().getEmail());
        assertEquals("CUSTOMER", regCaptor.getValue().getRole());
        verify(response).sendRedirect("/");
    }
}
