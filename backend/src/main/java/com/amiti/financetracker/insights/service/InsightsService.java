package com.amiti.financetracker.insights.service;

import com.amiti.financetracker.accounts.service.AccountService;
import com.amiti.financetracker.budgets.dto.BudgetDtos.BudgetResponse;
import com.amiti.financetracker.budgets.service.BudgetService;
import com.amiti.financetracker.domain.entity.TransactionEntity;
import com.amiti.financetracker.insights.dto.InsightDtos.HealthFactor;
import com.amiti.financetracker.insights.dto.InsightDtos.HealthScoreResponse;
import com.amiti.financetracker.insights.dto.InsightDtos.InsightCardResponse;
import com.amiti.financetracker.insights.dto.InsightDtos.InsightSummaryResponse;
import com.amiti.financetracker.reports.dto.ReportDtos.ReportFilter;
import com.amiti.financetracker.reports.dto.ReportDtos.ReportSummaryResponse;
import com.amiti.financetracker.reports.service.ReportService;
import com.amiti.financetracker.transactions.service.TransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InsightsService {
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final AccountService accountService;
    private final ReportService reportService;

    public InsightsService(TransactionService transactionService, BudgetService budgetService, AccountService accountService, ReportService reportService) {
        this.transactionService = transactionService;
        this.budgetService = budgetService;
        this.accountService = accountService;
        this.reportService = reportService;
    }

    public HealthScoreResponse healthScore(UUID userId) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        BigDecimal currentIncome = totalByType(userId, currentMonth, "INCOME");
        BigDecimal currentExpense = totalByType(userId, currentMonth, "EXPENSE");

        int savingsScore = scaleTo100(rateValue(currentIncome.subtract(currentExpense), currentIncome));
        int stabilityScore = expenseStabilityScore(userId);
        int budgetScore = budgetAdherenceScore(userId, currentMonth);
        int bufferScore = cashBufferScore(userId, currentExpense);

        int score = Math.min(100, Math.max(0, (int) Math.round((savingsScore * 0.30) + (stabilityScore * 0.20) + (budgetScore * 0.25) + (bufferScore * 0.25))));
        List<HealthFactor> factors = List.of(
                new HealthFactor("Savings rate", savingsScore, percentage(currentIncome.subtract(currentExpense), currentIncome) + " of income saved this month."),
                new HealthFactor("Expense stability", stabilityScore, "Compared against the previous three months of expense movement."),
                new HealthFactor("Budget adherence", budgetScore, "Based on current budget thresholds and overspend risk."),
                new HealthFactor("Cash buffer", bufferScore, "Balance coverage against a month of expenses.")
        );

        List<String> suggestions = new ArrayList<>();
        if (savingsScore < 50) suggestions.add("Reduce discretionary spending or increase income to improve monthly savings rate.");
        if (budgetScore < 60) suggestions.add("Review categories nearing or exceeding budget thresholds.");
        if (bufferScore < 50) suggestions.add("Build a larger cash buffer to cover at least one month of expenses.");
        if (stabilityScore < 50) suggestions.add("Your expense pattern is volatile; review recurring costs and large one-off spends.");
        if (suggestions.isEmpty()) suggestions.add("Financial health is stable. Keep contributions and spending habits consistent.");

        return new HealthScoreResponse(score, factors, suggestions);
    }

    public InsightSummaryResponse insights(UUID userId) {
        return insights(userId, "THIS_MONTH", null, null, null, null, "ALL");
    }

    public InsightSummaryResponse insights(UUID userId, String range, LocalDate startDate, LocalDate endDate, UUID accountId, UUID categoryId, String type) {
        ReportFilter currentFilter = new ReportFilter(range == null ? "THIS_MONTH" : range, startDate, endDate, accountId, categoryId, type == null ? "ALL" : type);
        ReportFilter previousFilter = previousComparable(currentFilter);
        ReportSummaryResponse current = reportService.summary(userId, currentFilter);
        ReportSummaryResponse previous = reportService.summary(userId, previousFilter);

        List<InsightCardResponse> cards = new ArrayList<>();
        if (!"None".equals(current.topCategory())) {
            cards.add(new InsightCardResponse("Top spend category", current.topCategory() + " is leading in the selected period.", "INFO"));
        }
        if (previous.totalExpense().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal expenseDelta = current.totalExpense().subtract(previous.totalExpense()).multiply(BigDecimal.valueOf(100)).divide(previous.totalExpense(), 0, RoundingMode.HALF_UP);
            cards.add(new InsightCardResponse("Expense movement", "Your spending changed by " + expenseDelta + "% versus the previous comparable period.", expenseDelta.signum() > 0 ? "WARNING" : "SUCCESS"));
        }
        BigDecimal currentSavingsRate = rateValue(current.net(), current.totalIncome());
        BigDecimal previousSavingsRate = rateValue(previous.net(), previous.totalIncome());
        BigDecimal savingsDelta = currentSavingsRate.subtract(previousSavingsRate);
        cards.add(new InsightCardResponse("Savings momentum", savingsDelta.signum() >= 0 ? "You saved more than the previous comparable period." : "You saved less than the previous comparable period.", savingsDelta.signum() >= 0 ? "SUCCESS" : "WARNING"));

        return new InsightSummaryResponse(currentSavingsRate.setScale(2, RoundingMode.HALF_UP), previousSavingsRate.setScale(2, RoundingMode.HALF_UP), cards);
    }

    private ReportFilter previousComparable(ReportFilter filter) {
        LocalDate today = LocalDate.now();
        return switch (filter.range()) {
            case "LAST_3_MONTHS" -> new ReportFilter("CUSTOM", today.minusMonths(5).withDayOfMonth(1), today.minusMonths(3).withDayOfMonth(today.minusMonths(3).lengthOfMonth()), filter.accountId(), filter.categoryId(), filter.type());
            case "THIS_YEAR" -> new ReportFilter("CUSTOM", today.minusYears(1).withDayOfYear(1), today.minusYears(1), filter.accountId(), filter.categoryId(), filter.type());
            case "CUSTOM" -> {
                LocalDate start = filter.startDate() == null ? today.withDayOfMonth(1) : filter.startDate();
                LocalDate end = filter.endDate() == null ? today : filter.endDate();
                long length = ChronoUnit.DAYS.between(start, end) + 1;
                yield new ReportFilter("CUSTOM", start.minusDays(length), end.minusDays(length), filter.accountId(), filter.categoryId(), filter.type());
            }
            default -> new ReportFilter("LAST_3_MONTHS", null, null, filter.accountId(), filter.categoryId(), filter.type());
        };
    }

    private int expenseStabilityScore(UUID userId) {
        List<BigDecimal> monthlyExpenses = new ArrayList<>();
        YearMonth month = YearMonth.now().minusMonths(2);
        for (int i = 0; i < 3; i += 1) {
            monthlyExpenses.add(totalByType(userId, month.plusMonths(i), "EXPENSE"));
        }
        BigDecimal average = monthlyExpenses.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(Math.max(1, monthlyExpenses.size())), 2, RoundingMode.HALF_UP);
        if (average.compareTo(BigDecimal.ZERO) == 0) {
            return 70;
        }
        BigDecimal maxDeviation = monthlyExpenses.stream().map(value -> value.subtract(average).abs()).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal ratio = maxDeviation.multiply(BigDecimal.valueOf(100)).divide(average, 0, RoundingMode.HALF_UP);
        return Math.max(20, 100 - ratio.intValue());
    }

    private int budgetAdherenceScore(UUID userId, YearMonth currentMonth) {
        List<BudgetResponse> budgets = budgetService.list(userId, currentMonth.getMonthValue(), currentMonth.getYear());
        if (budgets.isEmpty()) {
            return 65;
        }
        double average = budgets.stream().mapToInt(item -> Math.min(item.percentUsed(), 140)).average().orElse(80);
        return Math.max(15, 110 - (int) average);
    }

    private int cashBufferScore(UUID userId, BigDecimal currentExpense) {
        BigDecimal totalBalance = accountService.list(userId).stream().map(item -> item.currentBalance() == null ? BigDecimal.ZERO : item.currentBalance()).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (currentExpense.compareTo(BigDecimal.ZERO) <= 0) {
            return totalBalance.compareTo(BigDecimal.ZERO) > 0 ? 85 : 50;
        }
        BigDecimal months = totalBalance.divide(currentExpense, 2, RoundingMode.HALF_UP);
        return Math.min(100, months.multiply(BigDecimal.valueOf(40)).intValue());
    }

    private BigDecimal totalByType(UUID userId, YearMonth month, String type) {
        return transactionService.findForPeriod(userId, month.atDay(1), month.atEndOfMonth()).stream()
                .filter(tx -> type.equals(tx.getType()))
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal rateValue(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private int scaleTo100(BigDecimal percent) {
        return Math.max(0, Math.min(100, percent.intValue()));
    }

    private String percentage(BigDecimal numerator, BigDecimal denominator) {
        return rateValue(numerator, denominator).setScale(0, RoundingMode.HALF_UP) + "%";
    }
}