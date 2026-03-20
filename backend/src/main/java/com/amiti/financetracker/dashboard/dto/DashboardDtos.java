package com.amiti.financetracker.dashboard.dto;

import com.amiti.financetracker.domain.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class DashboardDtos {
    private DashboardDtos() {
    }

    public record DashboardSummaryResponse(
            BigDecimal currentMonthIncome,
            BigDecimal currentMonthExpense,
            BigDecimal netBalance,
            BigDecimal savingsRate,
            int activeBudgets,
            int upcomingBills
    ) {
    }

    public record RecentTransactionItem(
            UUID id,
            String merchant,
            String category,
            TransactionType type,
            BigDecimal amount,
            LocalDate date
    ) {
    }

    public record RecurringPreviewItem(UUID id, String title, LocalDate nextRunDate, BigDecimal amount) {
    }

    public record BudgetProgressResponse(
            UUID id,
            String category,
            BigDecimal budgetAmount,
            BigDecimal actualSpend,
            int percentUsed
    ) {
    }

    public record GoalProgressItem(
            UUID id,
            String name,
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            LocalDate targetDate,
            int progressPercent
    ) {
    }
}
