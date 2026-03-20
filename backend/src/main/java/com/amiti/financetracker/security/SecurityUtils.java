package com.amiti.financetracker.security;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Authentication required");
        }
        return UUID.fromString(authentication.getName());
    }
}
