package com.amiti.financetracker.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ReportDtos {
    private ReportDtos() {
    }

    public record ReportFilter(
            String range,
            LocalDate startDate,
            LocalDate endDate,
            UUID accountId,
            UUID categoryId,
            String type
    ) {
    }

    public record ReportSummaryResponse(
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal net,
            long transactionCount,
            String topCategory
    ) {
    }

    public record ReportTransactionRow(
            UUID id,
            LocalDate date,
            String merchant,
            String category,
            String account,
            String type,
            BigDecimal amount,
            String note
    ) {
    }

    public record CategorySpendPoint(String category, BigDecimal amount) {
    }

    public record IncomeExpenseTrendPoint(String period, BigDecimal income, BigDecimal expense) {
    }

    public record AccountBalanceTrendPoint(String period, BigDecimal balance) {
    }

    public record SavingsRateTrendPoint(String period, BigDecimal savingsRate) {
    }

    public record NetWorthPoint(String period, BigDecimal netWorth) {
    }
}