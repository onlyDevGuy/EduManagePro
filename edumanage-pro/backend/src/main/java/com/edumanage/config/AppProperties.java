package com.edumanage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Stripe stripe, Bootstrap bootstrap, Login login) {

    public record Jwt(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Stripe(String secretKey, String webhookSecret, String successUrl, String cancelUrl) {
        public boolean isConfigured() {
            return secretKey != null && !secretKey.isBlank();
        }
    }

    public record Bootstrap(boolean enabled) {}

    /** Failed sign-ins allowed per account before it is locked for {@code lockout}. */
    public record Login(int maxFailures, Duration lockout) {}
}