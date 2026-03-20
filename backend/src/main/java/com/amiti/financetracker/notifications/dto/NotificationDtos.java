package com.amiti.financetracker.notifications.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class NotificationDtos {
    private NotificationDtos() {
    }

    public enum NotificationSeverity {
        INFO,
        WARNING,
        SUCCESS,
        DANGER
    }

    public record NotificationResponse(
            UUID id,
            NotificationSeverity severity,
            String message,
            String source,
            LocalDateTime createdAt
    ) {
    }

    public record NotificationClearRequest(
            List<UUID> ids
    ) {
    }
}
