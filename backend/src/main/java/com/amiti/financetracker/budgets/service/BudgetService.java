package com.amiti.financetracker.budgets.service;

import com.amiti.financetracker.accounts.service.AccountActivityService;
import com.amiti.financetracker.accounts.service.SharedAccountService;
import com.amiti.financetracker.budgets.dto.BudgetDtos.BudgetRequest;
import com.amiti.financetracker.budgets.dto.BudgetDtos.BudgetResponse;
import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.domain.entity.BudgetEntity;
import com.amiti.financetracker.domain.repository.AccountRepository;
import com.amiti.financetracker.domain.repository.BudgetRepository;
import com.amiti.financetracker.domain.repository.CategoryRepository;
import com.amiti.financetracker.transactions.service.TransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final SharedAccountService sharedAccountService;
    private final AccountActivityService accountActivityService;

    public BudgetService(BudgetRepository budgetRepository, CategoryRepository categoryRepository, TransactionService transactionService, AccountRepository accountRepository, SharedAccountService sharedAccountService, AccountActivityService accountActivityService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
        this.sharedAccountService = sharedAccountService;
        this.accountActivityService = accountActivityService;
    }

    public List<BudgetResponse> list(UUID userId, int month, int year) {
        Set<UUID> accessible = sharedAccountService.accessibleAccountIds(userId);
        return budgetRepository.findByMonthAndYear(month, year).stream()
                .filter(budget -> userId.equals(budget.getUserId()) || (budget.getAccountId() != null && accessible.contains(budget.getAccountId())))
                .map(b -> toResponse(userId, b)).toList();
    }

    @Transactional
    public BudgetResponse create(UUID userId, BudgetRequest request) {
        BudgetEntity entity = new BudgetEntity();
        entity.setUserId(userId);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        apply(userId, entity, request);
        BudgetEntity saved = budgetRepository.save(entity);
        if (saved.getAccountId() != null) {
            accountActivityService.log(saved.getAccountId(), userId, "BUDGET_CREATED", "BUDGET", saved.getId(), "Created shared budget for " + toResponse(userId, saved).category());
        }
        return toResponse(userId, saved);
    }

    @Transactional
    public BudgetResponse update(UUID userId, UUID budgetId, BudgetRequest request) {
        BudgetEntity entity = budgetRepository.findById(budgetId).orElseThrow(() -> new NotFoundException("Budget not found"));
        assertCanAccess(userId, entity);
        apply(userId, entity, request);
        entity.setUpdatedBy(userId);
        BudgetEntity saved = budgetRepository.save(entity);
        if (saved.getAccountId() != null) {
            accountActivityService.log(saved.getAccountId(), userId, "BUDGET_UPDATED", "BUDGET", saved.getId(), "Updated shared budget for " + toResponse(userId, saved).category());
        }
        return toResponse(userId, saved);
    }

    @Transactional
    public void delete(UUID userId, UUID budgetId) {
        BudgetEntity entity = budgetRepository.findById(budgetId).orElseThrow(() -> new NotFoundException("Budget not found"));
        assertCanAccess(userId, entity);
        if (entity.getAccountId() != null) {
            accountActivityService.log(entity.getAccountId(), userId, "BUDGET_DELETED", "BUDGET", entity.getId(), "Removed shared budget");
        }
        budgetRepository.delete(entity);
    }

    private void apply(UUID userId, BudgetEntity entity, BudgetRequest request) {
        categoryRepository.findById(request.categoryId())
                .filter(category -> userId.equals(category.getUserId()) || category.getUserId() == null)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (request.accountId() != null) {
            accountRepository.findById(request.accountId()).orElseThrow(() -> new NotFoundException("Account not found"));
            sharedAccountService.assertCanEdit(userId, request.accountId());
        }
        entity.setCategoryId(request.categoryId());
        entity.setAccountId(request.accountId());
        entity.setMonth(request.month());
        entity.setYear(request.year());
        entity.setAmount(request.amount());
        entity.setAlertThresholdPercent(request.alertThresholdPercent());
    }

    private void assertCanAccess(UUID userId, BudgetEntity entity) {
        if (userId.equals(entity.getUserId())) {
            return;
        }
        if (entity.getAccountId() != null && sharedAccountService.canEdit(userId, entity.getAccountId())) {
            return;
        }
        throw new NotFoundException("Budget not found");
    }

    private BudgetResponse toResponse(UUID userId, BudgetEntity entity) {
        LocalDate start = LocalDate.of(entity.getYear(), entity.getMonth(), 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        BigDecimal actual = transactionService.findForPeriod(userId, start, end).stream()
                .filter(tx -> "EXPENSE".equals(tx.getType()) && entity.getCategoryId().equals(tx.getCategoryId()))
                .filter(tx -> entity.getAccountId() == null || entity.getAccountId().equals(tx.getAccountId()))
                .map(tx -> tx.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int percent = entity.getAmount().compareTo(BigDecimal.ZERO) == 0 ? 0 : actual.multiply(BigDecimal.valueOf(100)).divide(entity.getAmount(), 0, RoundingMode.HALF_UP).intValue();
        String category = categoryRepository.findById(entity.getCategoryId()).map(c -> c.getName()).orElse("Unknown");
        String account = entity.getAccountId() == null ? "All accessible accounts" : accountRepository.findById(entity.getAccountId()).map(a -> a.getName()).orElse("Unknown");
        boolean shared = entity.getAccountId() != null && !userId.equals(entity.getUserId());
        return new BudgetResponse(entity.getId(), entity.getCategoryId(), category, entity.getAccountId(), account, entity.getMonth(), entity.getYear(), entity.getAmount(), entity.getAlertThresholdPercent(), actual, percent, shared);
    }
}