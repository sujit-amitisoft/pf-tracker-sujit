package com.amiti.financetracker.accounts.dto;

import com.amiti.financetracker.domain.model.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountDtos {
    private AccountDtos() {
    }

    public record AccountResponse(
            UUID id,
            String name,
            AccountType type,
            BigDecimal currentBalance,
            String institutionName,
            LocalDateTime lastUpdatedAt,
            boolean shared,
            String accessRole,
            int memberCount
    ) {
    }

    public record AccountRequest(
            @NotBlank String name,
            @NotNull AccountType type,
            @DecimalMin("0.0") BigDecimal openingBalance,
            String institutionName
    ) {
    }

    public record TransferRequest(
            @NotNull UUID sourceAccountId,
            @NotNull UUID destinationAccountId,
            @DecimalMin("0.01") BigDecimal amount
    ) {
    }
}