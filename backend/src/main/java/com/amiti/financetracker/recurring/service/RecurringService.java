package com.amiti.financetracker.recurring.service;

import com.amiti.financetracker.accounts.service.SharedAccountService;
import com.amiti.financetracker.common.BadRequestException;
import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.domain.entity.RecurringTransactionEntity;
import com.amiti.financetracker.domain.entity.TransactionEntity;
import com.amiti.financetracker.domain.repository.AccountRepository;
import com.amiti.financetracker.domain.repository.CategoryRepository;
import com.amiti.financetracker.domain.repository.RecurringTransactionRepository;
import com.amiti.financetracker.domain.repository.TransactionRepository;
import com.amiti.financetracker.recurring.dto.RecurringDtos.RecurringRequest;
import com.amiti.financetracker.recurring.dto.RecurringDtos.RecurringResponse;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringService {
    private final RecurringTransactionRepository recurringRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final SharedAccountService sharedAccountService;

    public RecurringService(
            RecurringTransactionRepository recurringRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            SharedAccountService sharedAccountService
    ) {
        this.recurringRepository = recurringRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.sharedAccountService = sharedAccountService;
    }

    public List<RecurringResponse> list(UUID userId) {
        Map<UUID, RecurringTransactionEntity> recurringItems = new LinkedHashMap<>();
        recurringRepository.findByUserIdOrderByNextRunDateAsc(userId).forEach(item -> recurringItems.put(item.getId(), item));
        var accessibleAccounts = sharedAccountService.accessibleAccountIds(userId);
        if (!accessibleAccounts.isEmpty()) {
            recurringRepository.findByAccountIdInOrderByNextRunDateAsc(accessibleAccounts).forEach(item -> recurringItems.put(item.getId(), item));
        }
        return recurringItems.values().stream()
                .sorted(Comparator.comparing(RecurringTransactionEntity::getNextRunDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RecurringResponse create(UUID userId, RecurringRequest request) {
        RecurringTransactionEntity entity = new RecurringTransactionEntity();
        entity.setUserId(userId);
        apply(entity, request, userId);
        return toResponse(recurringRepository.save(entity));
    }

    @Transactional
    public RecurringResponse update(UUID userId, UUID recurringId, RecurringRequest request) {
        RecurringTransactionEntity entity = recurringRepository.findById(recurringId).orElseThrow(() -> new NotFoundException("Recurring item not found"));
        assertCanEdit(userId, entity);
        apply(entity, request, userId);
        return toResponse(recurringRepository.save(entity));
    }

    @Transactional
    public void delete(UUID userId, UUID recurringId) {
        RecurringTransactionEntity entity = recurringRepository.findById(recurringId).orElseThrow(() -> new NotFoundException("Recurring item not found"));
        assertCanEdit(userId, entity);
        recurringRepository.delete(entity);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void generateDueTransactions() {
        for (RecurringTransactionEntity recurring : recurringRepository.findByPausedFalseAndAutoCreateTransactionTrueAndNextRunDateLessThanEqual(LocalDate.now())) {
            TransactionEntity tx = new TransactionEntity();
            tx.setUserId(recurring.getUserId());
            tx.setAccountId(recurring.getAccountId());
            tx.setCategoryId(recurring.getCategoryId());
            tx.setRecurringTransactionId(recurring.getId());
            tx.setType(recurring.getType());
            tx.setAmount(recurring.getAmount());
            tx.setTransactionDate(recurring.getNextRunDate());
            tx.setMerchant(recurring.getTitle());
            tx.setNote("Auto-generated recurring transaction");
            transactionRepository.save(tx);
            recurring.setNextRunDate(nextDate(recurring));
            recurringRepository.save(recurring);
        }
    }

    private void apply(RecurringTransactionEntity entity, RecurringRequest request, UUID userId) {
        sharedAccountService.assertCanEdit(userId, request.accountId());
        entity.setTitle(request.title());
        entity.setType(request.type().name());
        entity.setAmount(request.amount());
        entity.setFrequency(request.frequency().name());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setNextRunDate(request.startDate());
        entity.setAutoCreateTransaction(request.autoCreateTransaction());
        entity.setPaused(false);
        entity.setAccountId(request.accountId());
        entity.setCategoryId(request.categoryId());
    }

    private void assertCanEdit(UUID userId, RecurringTransactionEntity entity) {
        if (userId.equals(entity.getUserId())) {
            return;
        }
        if (entity.getAccountId() != null && sharedAccountService.canEdit(userId, entity.getAccountId())) {
            return;
        }
        throw new BadRequestException("You do not have permission to modify this account");
    }

    private LocalDate nextDate(RecurringTransactionEntity recurring) {
        return switch (recurring.getFrequency()) {
            case "DAILY" -> recurring.getNextRunDate().plusDays(1);
            case "WEEKLY" -> recurring.getNextRunDate().plusWeeks(1);
            case "YEARLY" -> recurring.getNextRunDate().plusYears(1);
            default -> recurring.getNextRunDate().plusMonths(1);
        };
    }

    private RecurringResponse toResponse(RecurringTransactionEntity entity) {
        String category = entity.getCategoryId() == null ? null : categoryRepository.findById(entity.getCategoryId()).map(c -> c.getName()).orElse(null);
        String account = entity.getAccountId() == null ? null : accountRepository.findById(entity.getAccountId()).map(a -> a.getName()).orElse(null);
        return new RecurringResponse(
                entity.getId(),
                entity.getTitle(),
                com.amiti.financetracker.domain.model.TransactionType.valueOf(entity.getType()),
                entity.getAmount(),
                category,
                account,
                com.amiti.financetracker.domain.model.RecurringFrequency.valueOf(entity.getFrequency()),
                entity.getNextRunDate(),
                entity.isAutoCreateTransaction(),
                entity.isPaused()
        );
    }
}