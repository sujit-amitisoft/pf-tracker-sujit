package com.amiti.financetracker.reports.service;

import com.amiti.financetracker.accounts.service.SharedAccountService;
import com.amiti.financetracker.domain.entity.TransactionEntity;
import com.amiti.financetracker.domain.repository.AccountRepository;
import com.amiti.financetracker.domain.repository.CategoryRepository;
import com.amiti.financetracker.reports.dto.ReportDtos.AccountBalanceTrendPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.CategorySpendPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.IncomeExpenseTrendPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.NetWorthPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.ReportFilter;
import com.amiti.financetracker.reports.dto.ReportDtos.ReportSummaryResponse;
import com.amiti.financetracker.reports.dto.ReportDtos.ReportTransactionRow;
import com.amiti.financetracker.reports.dto.ReportDtos.SavingsRateTrendPoint;
import com.amiti.financetracker.transactions.service.TransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final TransactionService transactionService;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final SharedAccountService sharedAccountService;

    public ReportService(TransactionService transactionService, CategoryRepository categoryRepository, AccountRepository accountRepository, SharedAccountService sharedAccountService) {
        this.transactionService = transactionService;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.sharedAccountService = sharedAccountService;
    }

    public ReportSummaryResponse summary(UUID userId, ReportFilter filter) {
        List<TransactionEntity> transactions = filteredTransactions(userId, filter);
        BigDecimal income = sumByType(transactions, "INCOME");
        BigDecimal expense = sumByType(transactions, "EXPENSE");
        String topCategory = transactions.stream().filter(tx -> "EXPENSE".equals(tx.getType()))
                .collect(Collectors.groupingBy(tx -> resolveCategoryName(tx.getCategoryId()), Collectors.mapping(TransactionEntity::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("None");
        return new ReportSummaryResponse(income, expense, income.subtract(expense).setScale(2, RoundingMode.HALF_UP), transactions.size(), topCategory);
    }

    public List<ReportTransactionRow> transactions(UUID userId, ReportFilter filter) {
        return filteredTransactions(userId, filter).stream().map(tx -> new ReportTransactionRow(
                tx.getId(),
                tx.getTransactionDate(),
                tx.getMerchant(),
                resolveCategoryName(tx.getCategoryId()),
                accountRepository.findById(tx.getAccountId()).map(account -> account.getName()).orElse("Unknown"),
                tx.getType(),
                tx.getAmount().setScale(2, RoundingMode.HALF_UP),
                tx.getNote()
        )).toList();
    }

    public byte[] exportPdf(UUID userId, ReportFilter filter) {
        ReportSummaryResponse summary = summary(userId, filter);
        List<ReportTransactionRow> rows = transactions(userId, filter);
        List<String> lines = new ArrayList<>();
        lines.add("Finance Report");
        lines.add("Range: " + (filter.range() == null ? "THIS_MONTH" : filter.range()));
        lines.add("Account filter: " + (filter.accountId() == null ? "All" : accountRepository.findById(filter.accountId()).map(account -> account.getName()).orElse("Unknown")));
        lines.add("Category filter: " + (filter.categoryId() == null ? "All" : resolveCategoryName(filter.categoryId())));
        lines.add("Type filter: " + (filter.type() == null ? "ALL" : filter.type()));
        if (filter.startDate() != null || filter.endDate() != null) {
            lines.add("Dates: " + (filter.startDate() == null ? "-" : filter.startDate()) + " to " + (filter.endDate() == null ? "-" : filter.endDate()));
        }
        lines.add("");
        lines.add("Totals");
        lines.add("Income: $" + summary.totalIncome());
        lines.add("Expense: $" + summary.totalExpense());
        lines.add("Net: $" + summary.net());
        lines.add("Transactions: " + summary.transactionCount());
        lines.add("Top Category: " + summary.topCategory());
        lines.add("");
        lines.add("Transactions");
        lines.add("Date | Merchant | Category | Account | Type | Amount | Note");
        rows.stream().limit(120).forEach(row -> lines.add(row.date() + " | " + safe(row.merchant()) + " | " + row.category() + " | " + row.account() + " | " + row.type() + " | $" + row.amount() + " | " + safe(row.note())));
        return PdfReportRenderer.render(lines);
    }

    public List<CategorySpendPoint> categorySpend(UUID userId) { return categorySpend(userId, defaultFilter()); }
    public List<CategorySpendPoint> categorySpend(UUID userId, ReportFilter filter) {
        return filteredTransactions(userId, filter).stream()
                .filter(tx -> "EXPENSE".equals(tx.getType()))
                .collect(Collectors.groupingBy(tx -> resolveCategoryName(tx.getCategoryId()), Collectors.mapping(TransactionEntity::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .entrySet().stream()
                .map(entry -> new CategorySpendPoint(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP)))
                .sorted((left, right) -> right.amount().compareTo(left.amount()))
                .toList();
    }

    public List<IncomeExpenseTrendPoint> incomeExpense(UUID userId) { return incomeExpense(userId, defaultFilter()); }
    public List<IncomeExpenseTrendPoint> incomeExpense(UUID userId, ReportFilter filter) { return buildTrend(userId, filter); }
    public List<AccountBalanceTrendPoint> accountBalance(UUID userId) { return accountBalance(userId, defaultFilter()); }
    public List<AccountBalanceTrendPoint> accountBalance(UUID userId, ReportFilter filter) {
        List<AccountBalanceTrendPoint> points = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (IncomeExpenseTrendPoint point : buildTrend(userId, filter)) {
            running = running.add(point.income()).subtract(point.expense());
            points.add(new AccountBalanceTrendPoint(point.period(), running.setScale(2, RoundingMode.HALF_UP)));
        }
        return points;
    }
    public List<SavingsRateTrendPoint> savingsRateTrend(UUID userId) { return savingsRateTrend(userId, defaultFilter()); }
    public List<SavingsRateTrendPoint> savingsRateTrend(UUID userId, ReportFilter filter) {
        List<SavingsRateTrendPoint> points = new ArrayList<>();
        for (IncomeExpenseTrendPoint point : buildTrend(userId, new ReportFilter(filter.range(), filter.startDate(), filter.endDate(), filter.accountId(), filter.categoryId(), "ALL"))) {
            BigDecimal rate = point.income().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : point.income().subtract(point.expense()).multiply(BigDecimal.valueOf(100)).divide(point.income(), 2, RoundingMode.HALF_UP);
            points.add(new SavingsRateTrendPoint(point.period(), rate));
        }
        return points;
    }
    public List<NetWorthPoint> netWorth(UUID userId) { return netWorth(userId, defaultFilter()); }
    public List<NetWorthPoint> netWorth(UUID userId, ReportFilter filter) {
        BigDecimal opening = accountRepository.findAllById(selectedAccountIds(userId, filter.accountId())).stream().map(account -> account.getOpeningBalance() == null ? BigDecimal.ZERO : account.getOpeningBalance()).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<NetWorthPoint> points = new ArrayList<>();
        BigDecimal running = opening;
        for (IncomeExpenseTrendPoint point : buildTrend(userId, new ReportFilter(filter.range(), filter.startDate(), filter.endDate(), filter.accountId(), filter.categoryId(), "ALL"))) {
            running = running.add(point.income()).subtract(point.expense());
            points.add(new NetWorthPoint(point.period(), running.setScale(2, RoundingMode.HALF_UP)));
        }
        return points;
    }

    public String resolveCategoryName(UUID categoryId) {
        if (categoryId == null) return "Uncategorized";
        return categoryRepository.findById(categoryId).map(category -> category.getName()).orElse("Unknown");
    }

    public ReportFilter defaultFilter() { return new ReportFilter("THIS_MONTH", null, null, null, null, "ALL"); }

    private List<IncomeExpenseTrendPoint> buildTrend(UUID userId, ReportFilter filter) {
        List<IncomeExpenseTrendPoint> points = new ArrayList<>();
        for (YearMonth month : trendBuckets(filter)) {
            List<TransactionEntity> monthTransactions = filteredTransactions(userId, new ReportFilter("CUSTOM", month.atDay(1), month.atEndOfMonth(), filter.accountId(), filter.categoryId(), filter.type()));
            points.add(new IncomeExpenseTrendPoint(monthLabel(month), sumByType(monthTransactions, "INCOME"), sumByType(monthTransactions, "EXPENSE")));
        }
        return points;
    }

    private BigDecimal sumByType(List<TransactionEntity> transactions, String type) {
        return transactions.stream().filter(tx -> type.equals(tx.getType())).map(TransactionEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private List<TransactionEntity> filteredTransactions(UUID userId, ReportFilter filter) {
        LocalDate[] bounds = bounds(filter);
        return transactionService.findForPeriod(userId, bounds[0], bounds[1]).stream()
                .filter(tx -> filter.accountId() == null || filter.accountId().equals(tx.getAccountId()))
                .filter(tx -> filter.categoryId() == null || filter.categoryId().equals(tx.getCategoryId()))
                .filter(tx -> filter.type() == null || "ALL".equalsIgnoreCase(filter.type()) || filter.type().equalsIgnoreCase(tx.getType()))
                .toList();
    }

    private LocalDate[] bounds(ReportFilter filter) {
        LocalDate today = LocalDate.now();
        String range = filter.range() == null ? "THIS_MONTH" : filter.range();
        return switch (range) {
            case "LAST_3_MONTHS" -> new LocalDate[]{today.minusMonths(2).withDayOfMonth(1), today};
            case "THIS_YEAR" -> new LocalDate[]{today.withDayOfYear(1), today};
            case "CUSTOM" -> new LocalDate[]{filter.startDate() != null ? filter.startDate() : today.withDayOfMonth(1), filter.endDate() != null ? filter.endDate() : today};
            default -> new LocalDate[]{today.withDayOfMonth(1), today};
        };
    }

    private List<YearMonth> trendBuckets(ReportFilter filter) {
        LocalDate[] bounds = bounds(filter);
        YearMonth start = YearMonth.from(bounds[0]);
        YearMonth end = YearMonth.from(bounds[1]);
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = start;
        while (!current.isAfter(end)) {
            months.add(current);
            current = current.plusMonths(1);
        }
        return months;
    }

    private String monthLabel(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private List<UUID> selectedAccountIds(UUID userId, UUID selectedAccountId) {
        if (selectedAccountId != null) return List.of(selectedAccountId);
        return List.copyOf(sharedAccountService.accessibleAccountIds(userId));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}