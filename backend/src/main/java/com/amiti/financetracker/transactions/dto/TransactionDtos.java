package com.amiti.financetracker.transactions.dto;

import com.amiti.financetracker.domain.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class TransactionDtos {
    private TransactionDtos() {
    }

    public record TransactionResponse(
            UUID id,
            String merchant,
            String category,
            String account,
            TransactionType type,
            BigDecimal amount,
            LocalDate date,
            String note
    ) {
    }

    public record TransactionRequest(
            @NotNull TransactionType type,
            @DecimalMin("0.01") BigDecimal amount,
            @NotNull LocalDate date,
            UUID accountId,
            UUID categoryId,
            String merchant,
            String note,
            String paymentMethod,
            List<String> tags
    ) {
    }
}
