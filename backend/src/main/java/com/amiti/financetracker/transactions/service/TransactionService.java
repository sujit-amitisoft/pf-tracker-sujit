package com.amiti.financetracker.transactions.service;

import com.amiti.financetracker.accounts.service.AccountActivityService;
import com.amiti.financetracker.accounts.service.AccountService;
import com.amiti.financetracker.accounts.service.SharedAccountService;
import com.amiti.financetracker.common.BadRequestException;
import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.domain.entity.TransactionEntity;
import com.amiti.financetracker.domain.model.TransactionType;
import com.amiti.financetracker.domain.repository.AccountRepository;
import com.amiti.financetracker.domain.repository.CategoryRepository;
import com.amiti.financetracker.domain.repository.TransactionRepository;
import com.amiti.financetracker.rules.service.RuleService;
import com.amiti.financetracker.transactions.dto.TransactionDtos.TransactionImportRequest;
import com.amiti.financetracker.transactions.dto.TransactionDtos.TransactionRequest;
import com.amiti.financetracker.transactions.dto.TransactionDtos.TransactionResponse;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final AccountService accountService;
    private final RuleService ruleService;
    private final SharedAccountService sharedAccountService;
    private final AccountActivityService accountActivityService;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository, CategoryRepository categoryRepository, AccountService accountService, RuleService ruleService, SharedAccountService sharedAccountService, AccountActivityService accountActivityService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.accountService = accountService;
        this.ruleService = ruleService;
        this.sharedAccountService = sharedAccountService;
        this.accountActivityService = accountActivityService;
    }

    public List<TransactionResponse> list(UUID userId) {
        Set<UUID> accessible = sharedAccountService.accessibleAccountIds(userId);
        if (accessible.isEmpty()) {
            return List.of();
        }
        return transactionRepository.findByAccountIdInOrderByTransactionDateDescCreatedAtDesc(accessible).stream().map(this::toResponse).toList();
    }

    public TransactionResponse get(UUID userId, UUID transactionId) {
        TransactionEntity entity = transactionRepository.findById(transactionId).orElseThrow(() -> new NotFoundException("Transaction not found"));
        sharedAccountService.assertCanAccess(userId, entity.getAccountId());
        return toResponse(entity);
    }

    @Transactional
    public TransactionResponse create(UUID userId, TransactionRequest request) {
        return createInternal(userId, request, false);
    }

    @Transactional
    public List<TransactionResponse> importTransactions(UUID userId, TransactionImportRequest request) {
        return request.transactions().stream().map(item -> createInternal(userId, item, true)).toList();
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID transactionId, TransactionRequest request) {
        TransactionEntity entity = transactionRepository.findById(transactionId).orElseThrow(() -> new NotFoundException("Transaction not found"));
        sharedAccountService.assertCanEdit(userId, entity.getAccountId());
        TransactionRequest resolved = ruleService.applyRules(userId, request);
        validate(userId, resolved);
        if (!"TRANSFER".equals(entity.getType())) {
            accountService.applyTransactionEffect(userId, entity.getAccountId(), entity.getType(), entity.getAmount(), true);
        }
        apply(entity, resolved);
        TransactionEntity saved = transactionRepository.save(entity);
        if (!TransactionType.TRANSFER.equals(resolved.type())) {
            accountService.applyTransactionEffect(userId, resolved.accountId(), resolved.type().name(), resolved.amount(), false);
        }
        accountActivityService.log(saved.getAccountId(), userId, "TRANSACTION_UPDATED", "TRANSACTION", saved.getId(), "Updated transaction for " + (saved.getMerchant() == null ? "manual entry" : saved.getMerchant()));
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID userId, UUID transactionId) {
        TransactionEntity entity = transactionRepository.findById(transactionId).orElseThrow(() -> new NotFoundException("Transaction not found"));
        sharedAccountService.assertCanEdit(userId, entity.getAccountId());
        if (!"TRANSFER".equals(entity.getType())) {
            accountService.applyTransactionEffect(userId, entity.getAccountId(), entity.getType(), entity.getAmount(), true);
        }
        accountActivityService.log(entity.getAccountId(), userId, "TRANSACTION_DELETED", "TRANSACTION", entity.getId(), "Deleted transaction for " + (entity.getMerchant() == null ? "manual entry" : entity.getMerchant()));
        transactionRepository.delete(entity);
    }

    public List<TransactionEntity> findForPeriod(UUID userId, LocalDate start, LocalDate end) {
        Set<UUID> accessible = sharedAccountService.accessibleAccountIds(userId);
        if (accessible.isEmpty()) {
            return List.of();
        }
        return transactionRepository.findByAccountIdInAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(accessible, start, end);
    }

    private TransactionResponse createInternal(UUID userId, TransactionRequest request, boolean imported) {
        TransactionRequest resolved = ruleService.applyRules(userId, request);
        validate(userId, resolved);
        TransactionEntity entity = new TransactionEntity();
        entity.setUserId(userId);
        apply(entity, resolved);
        TransactionEntity saved = transactionRepository.save(entity);
        if (!TransactionType.TRANSFER.equals(resolved.type())) {
            accountService.applyTransactionEffect(userId, resolved.accountId(), resolved.type().name(), resolved.amount(), false);
        }
        accountActivityService.log(saved.getAccountId(), userId, imported ? "TRANSACTION_IMPORTED" : "TRANSACTION_CREATED", "TRANSACTION", saved.getId(), (imported ? "Imported" : "Added") + " transaction for " + (saved.getMerchant() == null ? "manual entry" : saved.getMerchant()));
        return toResponse(saved);
    }

    private void validate(UUID userId, TransactionRequest request) {
        if (request.accountId() == null) throw new BadRequestException("Account is required");
        accountRepository.findById(request.accountId()).orElseThrow(() -> new NotFoundException("Account not found"));
        sharedAccountService.assertCanEdit(userId, request.accountId());
        if (request.type() != TransactionType.TRANSFER && request.categoryId() == null) throw new BadRequestException("Category is required for non-transfer transactions");
        if (request.categoryId() != null) {
            categoryRepository.findById(request.categoryId())
                    .filter(category -> userId.equals(category.getUserId()) || category.getUserId() == null)
                    .orElseThrow(() -> new NotFoundException("Category not found"));
        }
    }

    private void apply(TransactionEntity entity, TransactionRequest request) {
        entity.setAccountId(request.accountId());
        entity.setCategoryId(request.categoryId());
        entity.setType(request.type().name());
        entity.setAmount(request.amount());
        entity.setTransactionDate(request.date());
        entity.setMerchant(request.merchant());
        entity.setNote(request.note());
        entity.setPaymentMethod(request.paymentMethod());
        entity.setTags(request.tags() == null ? null : String.join(",", request.tags()));
    }

    private TransactionResponse toResponse(TransactionEntity entity) {
        String accountName = accountRepository.findById(entity.getAccountId()).map(a -> a.getName()).orElse("Unknown");
        String categoryName = entity.getCategoryId() == null ? "Transfer" : categoryRepository.findById(entity.getCategoryId()).map(c -> c.getName()).orElse("Unknown");
        return new TransactionResponse(
                entity.getId(),
                entity.getMerchant(),
                categoryName,
                accountName,
                TransactionType.valueOf(entity.getType()),
                entity.getAmount(),
                entity.getTransactionDate(),
                entity.getNote(),
                parseTags(entity.getTags())
        );
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
