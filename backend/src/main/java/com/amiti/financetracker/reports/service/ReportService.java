package com.amiti.financetracker.reports.service;

import com.amiti.financetracker.reports.dto.ReportDtos.AccountBalanceTrendPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.CategorySpendPoint;
import com.amiti.financetracker.reports.dto.ReportDtos.IncomeExpenseTrendPoint;
import com.amiti.financetracker.transactions.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final TransactionService transactionService;

    public ReportService(TransactionService transactionService) { this.transactionService = transactionService; }

    public List<CategorySpendPoint> categorySpend(UUID userId) {
        LocalDate now = LocalDate.now();
        return transactionService.findForPeriod(userId, now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth())).stream()
                .filter(tx -> "EXPENSE".equals(tx.getType()))
                .map(tx -> transactionService.get(userId, tx.getId()))
                .collect(Collectors.groupingBy(tx -> tx.category(), Collectors.mapping(tx -> tx.amount(), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .entrySet().stream().map(entry -> new CategorySpendPoint(entry.getKey(), entry.getValue())).toList();
    }

    public List<IncomeExpenseTrendPoint> incomeExpense(UUID userId) {
        LocalDate now = LocalDate.now();
        List<IncomeExpenseTrendPoint> points = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            var txs = transactionService.findForPeriod(userId, date.withDayOfMonth(1), date.withDayOfMonth(date.lengthOfMonth()));
            BigDecimal income = txs.stream().filter(tx -> "INCOME".equals(tx.getType())).map(tx -> tx.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expense = txs.stream().filter(tx -> "EXPENSE".equals(tx.getType())).map(tx -> tx.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            points.add(new IncomeExpenseTrendPoint(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), income, expense));
        }
        return points;
    }

    public List<AccountBalanceTrendPoint> accountBalance(UUID userId) {
        List<AccountBalanceTrendPoint> points = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (IncomeExpenseTrendPoint point : incomeExpense(userId)) {
            running = running.add(point.income()).subtract(point.expense());
            points.add(new AccountBalanceTrendPoint(point.period(), running));
        }
        return points;
    }
}
