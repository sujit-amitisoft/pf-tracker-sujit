package com.amiti.financetracker.goals.dto;

import com.amiti.financetracker.domain.model.GoalStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class GoalDtos {
    private GoalDtos() {
    }

    public record GoalResponse(
            UUID id,
            String name,
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            LocalDate targetDate,
            GoalStatus status,
            int progressPercent,
            UUID linkedAccountId,
            String linkedAccountName,
            boolean shared
    ) {
    }

    public record GoalRequest(
            @NotBlank String name,
            @DecimalMin("0.01") BigDecimal targetAmount,
            LocalDate targetDate,
            UUID linkedAccountId,
            String icon,
            String color
    ) {
    }

    public record GoalAmountRequest(@DecimalMin("0.01") BigDecimal amount, UUID sourceAccountId) {
    }
}