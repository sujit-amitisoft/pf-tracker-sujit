package com.amiti.financetracker.forecast.service;

import com.amiti.financetracker.accounts.service.SharedAccountService;
import com.amiti.financetracker.domain.entity.RecurringTransactionEntity;
import com.amiti.financetracker.domain.entity.TransactionEntity;
import com.amiti.financetracker.domain.repository.AccountRepository;
import com.amiti.financetracker.domain.repository.RecurringTransactionRepository;
import com.amiti.financetracker.forecast.dto.ForecastDtos.ForecastDailyPoint;
import com.amiti.financetracker.forecast.dto.ForecastDtos.ForecastDailyResponse;
import com.amiti.financetracker.forecast.dto.ForecastDtos.ForecastMonthResponse;
import com.amiti.financetracker.transactions.service.TransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ForecastService {
    private final TransactionService transactionService;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final AccountRepository accountRepository;
    private final SharedAccountService sharedAccountService;

    public ForecastService(TransactionService transactionService, RecurringTransactionRepository recurringTransactionRepository, AccountRepository accountRepository, SharedAccountService sharedAccountService) {
        this.transactionService = transactionService;
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.accountRepository = accountRepository;
        this.sharedAccountService = sharedAccountService;
    }

    public ForecastMonthResponse month(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
        BigDecimal currentBalance = totalAccessibleBalance(userId);
        BigDecimal knownIncome = recurringBetween(userId, today, end, "INCOME");
        BigDecimal knownExpense = recurringBetween(userId, today, end, "EXPENSE");
        BigDecimal averageDailyNet = historicalAverageDailyNet(userId);
        int daysRemaining = (int) ChronoUnit.DAYS.between(today, end) + 1;
        BigDecimal heuristicTail = averageDailyNet.multiply(BigDecimal.valueOf(daysRemaining));
        BigDecimal projectedBalance = currentBalance.add(knownIncome).subtract(knownExpense).add(heuristicTail);
        BigDecimal safeToSpend = projectedBalance.max(BigDecimal.ZERO);
        String warning = projectedBalance.compareTo(BigDecimal.ZERO) < 0 ? "Negative balance likely before month end." : null;
        return new ForecastMonthResponse(scale(currentBalance), scale(projectedBalance), scale(knownIncome), scale(knownExpense), scale(averageDailyNet), scale(safeToSpend), warning, daysRemaining);
    }

    public ForecastDailyResponse daily(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
        BigDecimal running = totalAccessibleBalance(userId);
        BigDecimal averageDailyNet = historicalAverageDailyNet(userId);
        List<ForecastDailyPoint> points = new ArrayList<>();
        for (LocalDate date = today; !date.isAfter(end); date = date.plusDays(1)) {
            BigDecimal recurringIncome = recurringBetween(userId, date, date, "INCOME");
            BigDecimal recurringExpense = recurringBetween(userId, date, date, "EXPENSE");
            running = running.add(recurringIncome).subtract(recurringExpense).add(averageDailyNet);
            points.add(new ForecastDailyPoint(date, scale(running)));
        }
        return new ForecastDailyResponse(points);
    }

    private BigDecimal historicalAverageDailyNet(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusMonths(3).withDayOfMonth(1);
        List<TransactionEntity> transactions = transactionService.findForPeriod(userId, start, today);
        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal income = transactions.stream().filter(tx -> "INCOME".equals(tx.getType())).map(TransactionEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = transactions.stream().filter(tx -> "EXPENSE".equals(tx.getType())).map(TransactionEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long days = Math.max(1, ChronoUnit.DAYS.between(start, today) + 1);
        return income.subtract(expense).divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal recurringBetween(UUID userId, LocalDate start, LocalDate end, String type) {
        return recurringTransactionRepository.findByUserIdOrderByNextRunDateAsc(userId).stream()
                .filter(item -> !item.isPaused())
                .filter(item -> type.equals(item.getType()))
                .filter(item -> !item.getNextRunDate().isBefore(start) && !item.getNextRunDate().isAfter(end))
                .map(RecurringTransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalAccessibleBalance(UUID userId) {
        return accountRepository.findAllById(sharedAccountService.accessibleAccountIds(userId)).stream()
                .map(account -> account.getCurrentBalance() == null ? BigDecimal.ZERO : account.getCurrentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}