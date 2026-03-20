package com.amiti.financetracker.dashboard.service;

import com.amiti.financetracker.budgets.dto.BudgetDtos.BudgetResponse;
import com.amiti.financetracker.budgets.service.BudgetService;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.BudgetProgressResponse;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.DashboardSummaryResponse;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.GoalProgressItem;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.RecentTransactionItem;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.RecurringPreviewItem;
import com.amiti.financetracker.domain.model.TransactionType;
import com.amiti.financetracker.recurring.service.RecurringService;
import com.amiti.financetracker.transactions.service.TransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final RecurringService recurringService;

    public DashboardService(TransactionService transactionService, BudgetService budgetService, RecurringService recurringService) {
        this.transactionService = transactionService;
        this.budgetService = budgetService;
        this.recurringService = recurringService;
    }

    public DashboardSummaryResponse summary(UUID userId) {
        LocalDate now = LocalDate.now();
        var current = transactionService.findForPeriod(userId, now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth()));
        BigDecimal income = current.stream().filter(t -> "INCOME".equals(t.getType())).map(t -> t.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = current.stream().filter(t -> "EXPENSE".equals(t.getType())).map(t -> t.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = income.subtract(expense);
        BigDecimal savingsRate = income.signum() == 0 ? BigDecimal.ZERO : net.max(BigDecimal.ZERO).multiply(BigDecimal.valueOf(100)).divide(income, 1, RoundingMode.HALF_UP);
        return new DashboardSummaryResponse(income, expense, net, savingsRate, budgetService.list(userId, now.getMonthValue(), now.getYear()).size(), recurringService.list(userId).size());
    }

    public List<RecentTransactionItem> recent(UUID userId) {
        return transactionService.list(userId).stream().limit(5).map(tx -> new RecentTransactionItem(tx.id(), tx.merchant(), tx.category(), tx.type(), tx.amount(), tx.date())).toList();
    }

    public List<RecurringPreviewItem> upcoming(UUID userId) {
        return recurringService.list(userId).stream().limit(5).map(item -> new RecurringPreviewItem(item.id(), item.title(), item.nextRunDate(), item.amount())).toList();
    }

    public List<BudgetProgressResponse> budgetProgress(UUID userId) {
        LocalDate now = LocalDate.now();
        return budgetService.list(userId, now.getMonthValue(), now.getYear()).stream()
                .map(item -> new BudgetProgressResponse(item.id(), item.category(), item.amount(), item.actualSpend(), item.percentUsed()))
                .toList();
    }

    public List<GoalProgressItem> goalsSummary(UUID userId) {
        return List.of();
    }
}
