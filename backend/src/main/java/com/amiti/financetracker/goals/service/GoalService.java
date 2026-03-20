package com.amiti.financetracker.goals.service;

import com.amiti.financetracker.accounts.service.AccountService;
import com.amiti.financetracker.common.BadRequestException;
import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.domain.entity.GoalEntity;
import com.amiti.financetracker.domain.model.GoalStatus;
import com.amiti.financetracker.domain.repository.GoalRepository;
import com.amiti.financetracker.goals.dto.GoalDtos.GoalAmountRequest;
import com.amiti.financetracker.goals.dto.GoalDtos.GoalRequest;
import com.amiti.financetracker.goals.dto.GoalDtos.GoalResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {
    private final GoalRepository goalRepository;
    private final AccountService accountService;

    public GoalService(GoalRepository goalRepository, AccountService accountService) {
        this.goalRepository = goalRepository;
        this.accountService = accountService;
    }

    public List<GoalResponse> list(UUID userId) {
        return goalRepository.findByUserIdOrderByTargetDateAsc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public GoalResponse create(UUID userId, GoalRequest request) {
        GoalEntity entity = new GoalEntity();
        entity.setUserId(userId);
        apply(entity, request);
        return toResponse(goalRepository.save(entity));
    }

    @Transactional
    public GoalResponse update(UUID userId, UUID goalId, GoalRequest request) {
        GoalEntity entity = goalRepository.findByIdAndUserId(goalId, userId).orElseThrow(() -> new NotFoundException("Goal not found"));
        apply(entity, request);
        return toResponse(goalRepository.save(entity));
    }

    @Transactional
    public void delete(UUID userId, UUID goalId) {
        GoalEntity entity = goalRepository.findByIdAndUserId(goalId, userId).orElseThrow(() -> new NotFoundException("Goal not found"));
        goalRepository.delete(entity);
    }

    @Transactional
    public GoalResponse contribute(UUID userId, UUID goalId, GoalAmountRequest request) {
        GoalEntity entity = goalRepository.findByIdAndUserId(goalId, userId).orElseThrow(() -> new NotFoundException("Goal not found"));
        if (request.sourceAccountId() != null) {
            accountService.applyTransactionEffect(userId, request.sourceAccountId(), "EXPENSE", request.amount(), false);
        }
        entity.setCurrentAmount(entity.getCurrentAmount().add(request.amount()));
        syncStatus(entity);
        return toResponse(goalRepository.save(entity));
    }

    @Transactional
    public GoalResponse withdraw(UUID userId, UUID goalId, GoalAmountRequest request) {
        GoalEntity entity = goalRepository.findByIdAndUserId(goalId, userId).orElseThrow(() -> new NotFoundException("Goal not found"));
        if (entity.getCurrentAmount().compareTo(request.amount()) < 0) {
            throw new BadRequestException("Withdrawal exceeds current goal balance");
        }
        if (request.sourceAccountId() != null) {
            accountService.applyTransactionEffect(userId, request.sourceAccountId(), "INCOME", request.amount(), false);
        }
        entity.setCurrentAmount(entity.getCurrentAmount().subtract(request.amount()));
        syncStatus(entity);
        return toResponse(goalRepository.save(entity));
    }

    private void apply(GoalEntity entity, GoalRequest request) {
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

    private GoalResponse toResponse(GoalEntity entity) {
        int progress = entity.getTargetAmount().compareTo(BigDecimal.ZERO) == 0 ? 0 : entity.getCurrentAmount().multiply(BigDecimal.valueOf(100)).divide(entity.getTargetAmount(), 0, RoundingMode.HALF_UP).intValue();
        return new GoalResponse(entity.getId(), entity.getName(), entity.getTargetAmount(), entity.getCurrentAmount(), entity.getTargetDate(), GoalStatus.valueOf(entity.getStatus()), Math.min(progress, 100));
    }
}
