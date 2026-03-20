package com.amiti.financetracker.recurring.dto;

import com.amiti.financetracker.domain.model.RecurringFrequency;
import com.amiti.financetracker.domain.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class RecurringDtos {
    private RecurringDtos() {
    }

    public record RecurringResponse(
            UUID id,
            String title,
            TransactionType type,
            BigDecimal amount,
            String category,
            String account,
            RecurringFrequency frequency,
            LocalDate nextRunDate,
            boolean autoCreateTransaction,
            boolean paused
    ) {
    }

    public record RecurringRequest(
            @NotBlank String title,
            @NotNull TransactionType type,
            @DecimalMin("0.01") BigDecimal amount,
            UUID categoryId,
            UUID accountId,
            @NotNull RecurringFrequency frequency,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            boolean autoCreateTransaction
    ) {
    }
}
