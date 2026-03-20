package com.amiti.financetracker.budgets.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class BudgetDtos {
    private BudgetDtos() {
    }

    public record BudgetResponse(
            UUID id,
            UUID categoryId,
            String category,
            int month,
            int year,
            BigDecimal amount,
            int alertThresholdPercent,
            BigDecimal actualSpend,
            int percentUsed
    ) {
    }

    public record BudgetRequest(
            @NotNull UUID categoryId,
            @Min(1) @Max(12) int month,
            @Min(2024) int year,
            @DecimalMin("0.01") BigDecimal amount,
            @Min(1) @Max(120) int alertThresholdPercent
    ) {
    }
}
