package com.amiti.financetracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        String allowedOrigins,
        Jwt jwt,
        Scheduler scheduler
) {
    public record Jwt(String secret, long accessExpiration, long refreshExpiration) {
    }

    public record Scheduler(boolean recurringEnabled) {
    }
}
