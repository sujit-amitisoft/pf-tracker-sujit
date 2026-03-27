package com.amiti.financetracker.goals.service;

import com.amiti.financetracker.accounts.service.AccountActivityService;
import com.amiti.financetracker.accounts.service.AccountService;
import com.amiti.financetracker.accounts.service.SharedAccountService;
import com.amiti.financetracker.common.BadRequestException;
import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.domain.entity.GoalEntity;
import com.amiti.financetracker.domain.model.GoalStatus;
import com.amiti.financetracker.domain.repository.AccountRepository;
import com.amiti.financetracker.domain.repository.GoalRepository;
import com.amiti.financetracker.goals.dto.GoalDtos.GoalAmountRequest;
import com.amiti.financetracker.goals.dto.GoalDtos.GoalRequest;
import com.amiti.financetracker.goals.dto.GoalDtos.GoalResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {
    private final GoalRepository goalRepository;
    private final AccountService accountService;
    private final SharedAccountService sharedAccountService;
    private final AccountActivityService accountActivityService;
    private final AccountRepository accountRepository;

    public GoalService(GoalRepository goalRepository, AccountService accountService, SharedAccountService sharedAccountService, AccountActivityService accountActivityService, AccountRepository accountRepository) {
        this.goalRepository = goalRepository;
        this.accountService = accountService;
        this.sharedAccountService = sharedAccountService;
        this.accountActivityService = accountActivityService;
        this.accountRepository = accountRepository;
    }

    public List<GoalResponse> list(UUID userId) {
        Map<UUID, GoalEntity> goals = new LinkedHashMap<>();
        goalRepository.findByUserIdOrderByTargetDateAsc(userId).forEach(goal -> goals.put(goal.getId(), goal));
        var accessibleAccounts = sharedAccountService.accessibleAccountIds(userId);
        if (!accessibleAccounts.isEmpty()) {
            goalRepository.findByLinkedAccountIdInOrderByTargetDateAsc(accessibleAccounts).forEach(goal -> goals.put(goal.getId(), goal));
        }
        return goals.values().stream().sorted(Comparator.comparing(GoalEntity::getTargetDate, Comparator.nullsLast(Comparator.naturalOrder()))).map(goal -> toResponse(userId, goal)).toList();
    }

    @Transactional
    public GoalResponse create(UUID userId, GoalRequest request) {
        GoalEntity entity = new GoalEntity();
        entity.setUserId(userId);
        apply(userId, entity, request);
        GoalEntity saved = goalRepository.save(entity);
        log(saved, userId, "GOAL_CREATED", "Created goal " + saved.getName());
        return toResponse(userId, saved);
    }

    @Transactional
    public GoalResponse update(UUID userId, UUID goalId, GoalRequest request) {
        GoalEntity entity = goalRepository.findById(goalId).orElseThrow(() -> new NotFoundException("Goal not found"));
        assertCanEdit(userId, entity);
        apply(userId, entity, request);
        GoalEntity saved = goalRepository.save(entity);
        log(saved, userId, "GOAL_UPDATED", "Updated goal " + saved.getName());
        return toResponse(userId, saved);
    }

    @Transactional
    public void delete(UUID userId, UUID goalId) {
        GoalEntity entity = goalRepository.findById(goalId).orElseThrow(() -> new NotFoundException("Goal not found"));
        assertCanEdit(userId, entity);
        log(entity, userId, "GOAL_DELETED", "Deleted goal " + entity.getName());
        goalRepository.delete(entity);
    }

    @Transactional
    public GoalResponse contribute(UUID userId, UUID goalId, GoalAmountRequest request) {
        GoalEntity entity = goalRepository.findById(goalId).orElseThrow(() -> new NotFoundException("Goal not found"));
        assertCanEdit(userId, entity);
        if (request.sourceAccountId() != null) {
            accountService.applyTransactionEffect(userId, request.sourceAccountId(), "EXPENSE", request.amount(), false);
        }
        entity.setCurrentAmount(entity.getCurrentAmount().add(request.amount()));
        syncStatus(entity);
        GoalEntity saved = goalRepository.save(entity);
        log(saved, userId, "GOAL_CONTRIBUTION", "Contributed $" + request.amount() + " to goal " + saved.getName());
        return toResponse(userId, saved);
    }

    @Transactional
    public GoalResponse withdraw(UUID userId, UUID goalId, GoalAmountRequest request) {
        GoalEntity entity = goalRepository.findById(goalId).orElseThrow(() -> new NotFoundException("Goal not found"));
        assertCanEdit(userId, entity);
        if (entity.getCurrentAmount().compareTo(request.amount()) < 0) {
            throw new BadRequestException("Withdrawal exceeds current goal balance");
        }
        if (request.sourceAccountId() != null) {
            accountService.applyTransactionEffect(userId, request.sourceAccountId(), "INCOME", request.amount(), false);
        }
        entity.setCurrentAmount(entity.getCurrentAmount().subtract(request.amount()));
        syncStatus(entity);
        GoalEntity saved = goalRepository.save(entity);
        log(saved, userId, "GOAL_WITHDRAWAL", "Withdrew $" + request.amount() + " from goal " + saved.getName());
        return toResponse(userId, saved);
    }

    private void apply(UUID userId, GoalEntity entity, GoalRequest request) {
        if (request.linkedAccountId() != null) {
            sharedAccountService.assertCanEdit(userId, request.linkedAccountId());
        }
        entity.setName(request.name());
        entity.setTargetAmount(request.targetAmount());
        entity.setTargetDate(request.targetDate());
        entity.setLinkedAccountId(request.linkedAccountId());
        entity.setIcon(request.icon());
        entity.setColor(request.color());
        syncStatus(entity);
    }

    private void syncStatus(GoalEntity entity) {
        if (entity.getCurrentAmount() == null) {
            entity.setCurrentAmount(BigDecimal.ZERO);
        }
        entity.setStatus(entity.getCurrentAmount().compareTo(entity.getTargetAmount()) >= 0 ? GoalStatus.COMPLETED.name() : GoalStatus.ACTIVE.name());
    }

    private void assertCanEdit(UUID userId, GoalEntity entity) {
        if (userId.equals(entity.getUserId())) {
            return;
        }
        if (entity.getLinkedAccountId() != null && sharedAccountService.canEdit(userId, entity.getLinkedAccountId())) {
            return;
        }
        throw new BadRequestException("You do not have permission to modify this goal");
    }

    private void log(GoalEntity entity, UUID userId, String type, String summary) {
        if (entity.getLinkedAccountId() != null) {
            accountActivityService.log(entity.getLinkedAccountId(), userId, type, "GOAL", entity.getId(), summary);
        }
    }

    private GoalResponse toResponse(UUID userId, GoalEntity entity) {
        int progress = entity.getTargetAmount().compareTo(BigDecimal.ZERO) == 0 ? 0 : entity.getCurrentAmount().multiply(BigDecimal.valueOf(100)).divide(entity.getTargetAmount(), 0, RoundingMode.HALF_UP).intValue();
        String linkedAccountName = entity.getLinkedAccountId() == null ? null : accountRepository.findById(entity.getLinkedAccountId()).map(account -> account.getName()).orElse("Unknown");
        boolean shared = entity.getLinkedAccountId() != null && !userId.equals(entity.getUserId());
        return new GoalResponse(entity.getId(), entity.getName(), entity.getTargetAmount(), entity.getCurrentAmount(), entity.getTargetDate(), GoalStatus.valueOf(entity.getStatus()), Math.min(progress, 100), entity.getLinkedAccountId(), linkedAccountName, shared);
    }
}