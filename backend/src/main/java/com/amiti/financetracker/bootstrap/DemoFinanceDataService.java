package com.amiti.financetracker.bootstrap;

import com.amiti.financetracker.accounts.dto.AccountDtos.AccountResponse;
import com.amiti.financetracker.budgets.dto.BudgetDtos.BudgetResponse;
import com.amiti.financetracker.categories.dto.CategoryDtos.CategoryResponse;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.BudgetProgressResponse;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.DashboardSummaryResponse;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.GoalProgressItem;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.RecentTransactionItem;
import com.amiti.financetracker.dashboard.dto.DashboardDtos.RecurringPreviewItem;
import com.amiti.financetracker.domain.model.AccountType;
import com.amiti.financetracker.domain.model.CategoryType;
import com.amiti.financetracker.domain.model.GoalStatus;
import com.amiti.financetracker.domain.model.RecurringFrequency;
import com.amiti.financetracker.domain.model.TransactionType;
import com.amiti.financetracker.goals.dto.GoalDtos.GoalResponse;
import com.amiti.financetracker.notifications.dto.NotificationDtos.NotificationResponse;
import com.amiti.financetracker.notifications.dto.NotificationDtos.NotificationSeverity;
import com.amiti.financetracker.recurring.dto.RecurringDtos.RecurringResponse;
import com.amiti.financetracker.reports.dto.ReportDtos.AccountBalanceTrendPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.CategorySpendPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.IncomeExpenseTrendPoint;
import com.amiti.financetracker.transactions.dto.TransactionDtos.TransactionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DemoFinanceDataService {

    public DashboardSummaryResponse dashboardSummary() {
        return new DashboardSummaryResponse(
                new BigDecimal("2400.00"),
                new BigDecimal("1184.75"),
                new BigDecimal("1215.25"),
                new BigDecimal("78.50"),
                4,
                2
        );
    }

    public List<RecentTransactionItem> recentTransactions() {
        return List.of(
                new RecentTransactionItem(UUID.randomUUID(), "Grocery Mart", "Food", TransactionType.EXPENSE, new BigDecimal("42.00"), LocalDate.now().minusDays(1)),
                new RecentTransactionItem(UUID.randomUUID(), "Employer Inc", "Salary", TransactionType.INCOME, new BigDecimal("2400.00"), LocalDate.now().minusDays(2)),
                new RecentTransactionItem(UUID.randomUUID(), "Uber", "Transport", TransactionType.EXPENSE, new BigDecimal("11.50"), LocalDate.now().minusDays(3))
        );
    }

    public List<RecurringPreviewItem> upcomingRecurring() {
        return List.of(
                new RecurringPreviewItem(UUID.randomUUID(), "Netflix", LocalDate.now().plusDays(2), new BigDecimal("15.99")),
                new RecurringPreviewItem(UUID.randomUUID(), "Rent", LocalDate.now().plusDays(7), new BigDecimal("650.00"))
        );
    }

    public List<BudgetProgressResponse> budgetProgress() {
        return List.of(
                new BudgetProgressResponse(UUID.randomUUID(), "Food", new BigDecimal("800"), new BigDecimal("650"), 81),
                new BudgetProgressResponse(UUID.randomUUID(), "Transport", new BigDecimal("250"), new BigDecimal("120"), 48)
        );
    }

    public List<GoalProgressItem> goalSummary() {
        return List.of(
                new GoalProgressItem(UUID.randomUUID(), "Emergency Fund", new BigDecimal("100000"), new BigDecimal("45000"), LocalDate.of(2026, 12, 31), 45),
                new GoalProgressItem(UUID.randomUUID(), "Vacation", new BigDecimal("50000"), new BigDecimal("20000"), LocalDate.of(2026, 8, 30), 40)
        );
    }

    public List<NotificationResponse> notifications() {
        return List.of(
                new NotificationResponse(UUID.randomUUID(), NotificationSeverity.WARNING, "Food budget is at 81% for this month.", "budget", LocalDate.now().atStartOfDay()),
                new NotificationResponse(UUID.randomUUID(), NotificationSeverity.INFO, "Netflix renewal is due in 2 days.", "recurring", LocalDate.now().atStartOfDay())
        );
    }

    public List<AccountResponse> accounts() {
        return List.of(
                new AccountResponse(UUID.randomUUID(), "HDFC Bank", AccountType.BANK_ACCOUNT, new BigDecimal("15000"), "HDFC", LocalDate.now().atStartOfDay()),
                new AccountResponse(UUID.randomUUID(), "Credit Card", AccountType.CREDIT_CARD, new BigDecimal("-2200"), "Visa", LocalDate.now().atStartOfDay())
        );
    }

    public List<CategoryResponse> categories() {
        return List.of(
                new CategoryResponse(UUID.randomUUID(), "Food", CategoryType.EXPENSE, "#ffb347", "utensils", false),
                new CategoryResponse(UUID.randomUUID(), "Salary", CategoryType.INCOME, "#1f8a70", "briefcase", false)
        );
    }

    public List<TransactionResponse> transactions() {
        return List.of(
                new TransactionResponse(UUID.randomUUID(), "Grocery Mart", "Food", "HDFC Bank", TransactionType.EXPENSE, new BigDecimal("42.00"), LocalDate.now().minusDays(1), "Weekly groceries"),
                new TransactionResponse(UUID.randomUUID(), "Employer Inc", "Salary", "HDFC Bank", TransactionType.INCOME, new BigDecimal("2400.00"), LocalDate.now().minusDays(2), "Monthly salary")
        );
    }

    public List<BudgetResponse> budgets() {
        return List.of(
                new BudgetResponse(UUID.randomUUID(), UUID.randomUUID(), "Food", 3, 2026, new BigDecimal("800"), 80, new BigDecimal("650"), 81),
                new BudgetResponse(UUID.randomUUID(), UUID.randomUUID(), "Entertainment", 3, 2026, new BigDecimal("200"), 80, new BigDecimal("210"), 105)
        );
    }

    public List<GoalResponse> goals() {
        return List.of(
                new GoalResponse(UUID.randomUUID(), "Emergency Fund", new BigDecimal("100000"), new BigDecimal("45000"), LocalDate.of(2026, 12, 31), GoalStatus.ACTIVE, 45),
                new GoalResponse(UUID.randomUUID(), "Vacation", new BigDecimal("50000"), new BigDecimal("20000"), LocalDate.of(2026, 8, 30), GoalStatus.ACTIVE, 40)
        );
    }

    public List<RecurringResponse> recurringItems() {
        return List.of(
                new RecurringResponse(UUID.randomUUID(), "Netflix", TransactionType.EXPENSE, new BigDecimal("15.99"), "Entertainment", "Credit Card", RecurringFrequency.MONTHLY, LocalDate.now().plusDays(2), true, false),
                new RecurringResponse(UUID.randomUUID(), "Salary", TransactionType.INCOME, new BigDecimal("2400.00"), "Salary", "HDFC Bank", RecurringFrequency.MONTHLY, LocalDate.now().plusDays(13), true, false)
        );
    }

    public List<CategorySpendPoint> categorySpend() {
        return List.of(
                new CategorySpendPoint("Food", new BigDecimal("650")),
                new CategorySpendPoint("Rent", new BigDecimal("650")),
                new CategorySpendPoint("Transport", new BigDecimal("120"))
        );
    }

    public List<IncomeExpenseTrendPoint> incomeExpenseTrend() {
        return List.of(
                new IncomeExpenseTrendPoint("Jan", new BigDecimal("2200"), new BigDecimal("1450")),
                new IncomeExpenseTrendPoint("Feb", new BigDecimal("2300"), new BigDecimal("1520")),
                new IncomeExpenseTrendPoint("Mar", new BigDecimal("2400"), new BigDecimal("1184.75"))
        );
    }

    public List<AccountBalanceTrendPoint> accountBalanceTrend() {
        return List.of(
                new AccountBalanceTrendPoint("Jan", new BigDecimal("8200")),
                new AccountBalanceTrendPoint("Feb", new BigDecimal("9700")),
                new AccountBalanceTrendPoint("Mar", new BigDecimal("12150"))
        );
    }
}


