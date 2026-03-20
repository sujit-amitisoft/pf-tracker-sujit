package com.amiti.financetracker.budgets.service;

import com.amiti.financetracker.budgets.dto.BudgetDtos.BudgetRequest;
import com.amiti.financetracker.budgets.dto.BudgetDtos.BudgetResponse;
import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.domain.entity.BudgetEntity;
import com.amiti.financetracker.domain.repository.BudgetRepository;
import com.amiti.financetracker.domain.repository.CategoryRepository;
import com.amiti.financetracker.transactions.service.TransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;

    public BudgetService(BudgetRepository budgetRepository, CategoryRepository categoryRepository, TransactionService transactionService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionService = transactionService;
    }

    public List<BudgetResponse> list(UUID userId, int month, int year) {
        return budgetRepository.findByUserIdAndMonthAndYear(userId, month, year).stream().map(b -> toResponse(userId, b)).toList();
    }

    @Transactional
    public BudgetResponse create(UUID userId, BudgetRequest request) {
        BudgetEntity entity = new BudgetEntity();
        entity.setUserId(userId);
        apply(userId, entity, request);
        return toResponse(userId, budgetRepository.save(entity));
    }

    @Transactional
    public BudgetResponse update(UUID userId, UUID budgetId, BudgetRequest request) {
        BudgetEntity entity = budgetRepository.findByIdAndUserId(budgetId, userId).orElseThrow(() -> new NotFoundException("Budget not found"));
        apply(userId, entity, request);
        return toResponse(userId, budgetRepository.save(entity));
    }

    @Transactional
    public void delete(UUID userId, UUID budgetId) {
        BudgetEntity entity = budgetRepository.findByIdAndUserId(budgetId, userId).orElseThrow(() -> new NotFoundException("Budget not found"));
        budgetRepository.delete(entity);
    }

    private void apply(UUID userId, BudgetEntity entity, BudgetRequest request) {
        categoryRepository.findById(request.categoryId())
                .filter(category -> userId.equals(category.getUserId()) || category.getUserId() == null)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        entity.setCategoryId(request.categoryId());
        entity.setMonth(request.month());
        entity.setYear(request.year());
        entity.setAmount(request.amount());
        entity.setAlertThresholdPercent(request.alertThresholdPercent());
    }

    private BudgetResponse toResponse(UUID userId, BudgetEntity entity) {
        BigDecimal actual = transactionService.findForPeriod(userId, LocalDate.of(entity.getYear(), entity.getMonth(), 1), LocalDate.of(entity.getYear(), entity.getMonth(), 1).withDayOfMonth(LocalDate.of(entity.getYear(), entity.getMonth(), 1).lengthOfMonth()))
                .stream()
                .filter(tx -> "EXPENSE".equals(tx.getType()) && entity.getCategoryId().equals(tx.getCategoryId()))
                .map(tx -> tx.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int percent = entity.getAmount().compareTo(BigDecimal.ZERO) == 0 ? 0 : actual.multiply(BigDecimal.valueOf(100)).divide(entity.getAmount(), 0, RoundingMode.HALF_UP).intValue();
        String category = categoryRepository.findById(entity.getCategoryId()).map(c -> c.getName()).orElse("Unknown");
        return new BudgetResponse(entity.getId(), entity.getCategoryId(), category, entity.getMonth(), entity.getYear(), entity.getAmount(), entity.getAlertThresholdPercent(), actual, percent);
    }
}
