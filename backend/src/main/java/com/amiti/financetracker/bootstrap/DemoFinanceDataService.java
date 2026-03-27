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
                new BigDecimal("1112.00"),
                new BigDecimal("1288.00"),
                new BigDecimal("53.67"),
                2,
                1
        );
    }

    public List<RecentTransactionItem> recentTransactions() {
        return List.of(
                new RecentTransactionItem(UUID.randomUUID(), "Fresh Basket", "Food", TransactionType.EXPENSE, new BigDecimal("120.00"), LocalDate.now().minusDays(3)),
                new RecentTransactionItem(UUID.randomUUID(), "Uber", "Transport", TransactionType.EXPENSE, new BigDecimal("28.00"), LocalDate.now().minusDays(2)),
                new RecentTransactionItem(UUID.randomUUID(), "Power Grid", "Utilities", TransactionType.EXPENSE, new BigDecimal("64.00"), LocalDate.now().minusDays(1))
        );
    }

    public List<RecurringPreviewItem> upcomingRecurring() {
        return List.of(
                new RecurringPreviewItem(UUID.randomUUID(), "Netflix", LocalDate.now().plusDays(2), new BigDecimal("15.99"))
        );
    }

    public List<BudgetProgressResponse> budgetProgress() {
        return List.of(
                new BudgetProgressResponse(UUID.randomUUID(), "Food", new BigDecimal("300"), new BigDecimal("120"), 40),
                new BudgetProgressResponse(UUID.randomUUID(), "Transport", new BigDecimal("120"), new BigDecimal("28"), 23)
        );
    }

    public List<GoalProgressItem> goalSummary() {
        return List.of(
                new GoalProgressItem(UUID.randomUUID(), "Emergency Fund", new BigDecimal("10000"), new BigDecimal("4200"), LocalDate.now().plusMonths(8), 42)
        );
    }

    public List<NotificationResponse> notifications() {
        return List.of(
                new NotificationResponse(UUID.randomUUID(), NotificationSeverity.WARNING, "Grocery spike detected on Fresh Basket.", "budget", LocalDate.now().atStartOfDay()),
                new NotificationResponse(UUID.randomUUID(), NotificationSeverity.INFO, "Netflix renewal is due in 2 days.", "recurring", LocalDate.now().atStartOfDay())
        );
    }

    public List<AccountResponse> accounts() {
        return List.of(
                new AccountResponse(UUID.randomUUID(), "Primary Bank", AccountType.BANK_ACCOUNT, new BigDecimal("4500"), "Amiti Bank", LocalDate.now().atStartOfDay(), false, "OWNER", 1),
                new AccountResponse(UUID.randomUUID(), "Everyday Card", AccountType.CREDIT_CARD, new BigDecimal("-320"), "Amiti Card Services", LocalDate.now().atStartOfDay(), false, "OWNER", 1)
        );
    }

    public List<CategoryResponse> categories() {
        return List.of(
                new CategoryResponse(UUID.randomUUID(), "Food", CategoryType.EXPENSE, "#ffb347", "utensils", false),
                new CategoryResponse(UUID.randomUUID(), "Transport", CategoryType.EXPENSE, "#38bdf8", "bus", false),
                new CategoryResponse(UUID.randomUUID(), "Utilities", CategoryType.EXPENSE, "#f97316", "bolt", false),
                new CategoryResponse(UUID.randomUUID(), "Salary", CategoryType.INCOME, "#1f8a70", "briefcase", false)
        );
    }

    public List<TransactionResponse> transactions() {
        return List.of(
                new TransactionResponse(UUID.randomUUID(), "Fresh Basket", "Food", "Everyday Card", TransactionType.EXPENSE, new BigDecimal("120.00"), LocalDate.now().minusDays(3), "Groceries [Alert: Grocery spike]", List.of("groceries", "weekly")),
                new TransactionResponse(UUID.randomUUID(), "Uber", "Transport", "Everyday Card", TransactionType.EXPENSE, new BigDecimal("28.00"), LocalDate.now().minusDays(2), "Airport commute", List.of("travel")),
                new TransactionResponse(UUID.randomUUID(), "Salary Credit", "Salary", "Primary Bank", TransactionType.INCOME, new BigDecimal("2400.00"), LocalDate.now().minusDays(6), "Monthly salary", List.of("income", "payroll"))
        );
    }

    public List<BudgetResponse> budgets() {
        return List.of(
                new BudgetResponse(UUID.randomUUID(), UUID.randomUUID(), "Food", null, "All accessible accounts", LocalDate.now().getMonthValue(), LocalDate.now().getYear(), new BigDecimal("300"), 80, new BigDecimal("120"), 40, false),
                new BudgetResponse(UUID.randomUUID(), UUID.randomUUID(), "Transport", null, "All accessible accounts", LocalDate.now().getMonthValue(), LocalDate.now().getYear(), new BigDecimal("120"), 80, new BigDecimal("28"), 23, false)
        );
    }

    public List<GoalResponse> goals() {
        return List.of(
                new GoalResponse(UUID.randomUUID(), "Emergency Fund", new BigDecimal("10000"), new BigDecimal("4200"), LocalDate.now().plusMonths(8), GoalStatus.ACTIVE, 42, null, null, false)
        );
    }

    public List<RecurringResponse> recurringItems() {
        return List.of(
                new RecurringResponse(UUID.randomUUID(), "Netflix", TransactionType.EXPENSE, new BigDecimal("15.99"), "Subscriptions", "Everyday Card", RecurringFrequency.MONTHLY, LocalDate.now().plusDays(2), true, false)
        );
    }

    public List<CategorySpendPoint> categorySpend() {
        return List.of(
                new CategorySpendPoint("Rent", new BigDecimal("900")),
                new CategorySpendPoint("Food", new BigDecimal("120")),
                new CategorySpendPoint("Transport", new BigDecimal("28")),
                new CategorySpendPoint("Utilities", new BigDecimal("64"))
        );
    }

    public List<IncomeExpenseTrendPoint> incomeExpenseTrend() {
        return List.of(
                new IncomeExpenseTrendPoint("Feb", new BigDecimal("2200"), new BigDecimal("980")),
                new IncomeExpenseTrendPoint("Mar", new BigDecimal("2400"), new BigDecimal("1112"))
        );
    }

    public List<AccountBalanceTrendPoint> accountBalanceTrend() {
        return List.of(
                new AccountBalanceTrendPoint("Week 1", new BigDecimal("4500")),
                new AccountBalanceTrendPoint("Week 2", new BigDecimal("3920")),
                new AccountBalanceTrendPoint("Week 3", new BigDecimal("4688"))
        );
    }
}
