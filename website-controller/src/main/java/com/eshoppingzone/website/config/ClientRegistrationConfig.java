package com.eshoppingzone.website.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class ClientRegistrationConfig {

    private static final Logger log = LoggerFactory.getLogger(ClientRegistrationConfig.class);

    @Value("${GITHUB_CLIENT_ID:${spring.security.oauth2.client.registration.github.client-id:}}")
    private String githubClientId;

    @Value("${GITHUB_CLIENT_SECRET:${spring.security.oauth2.client.registration.github.client-secret:}}")
    private String githubClientSecret;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        String effectiveClientId = (githubClientId != null && !githubClientId.trim().isEmpty())
                ? githubClientId.trim()
                : "placeholder-unconfigured-client-id";
        String effectiveClientSecret = (githubClientSecret != null && !githubClientSecret.trim().isEmpty())
                ? githubClientSecret.trim()
                : "placeholder-unconfigured-client-secret";

        ClientRegistration registration = CommonOAuth2Provider.GITHUB.getBuilder("github")
                .clientId(effectiveClientId)
                .clientSecret(effectiveClientSecret)
                .scope("read:user", "user:email")
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();

        if ("placeholder-unconfigured-client-id".equals(effectiveClientId)) {
            log.warn("GITHUB_CLIENT_ID is not configured in environment. Live GitHub OAuth requires GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET.");
        } else {
            log.info("GitHub OAuth2 successfully configured with Client ID: {}", effectiveClientId);
        }

        return new InMemoryClientRegistrationRepository(registration);
    }
}
