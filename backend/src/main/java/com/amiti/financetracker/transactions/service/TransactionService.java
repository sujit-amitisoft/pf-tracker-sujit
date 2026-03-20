package com.amiti.financetracker.transactions.service;

import com.amiti.financetracker.accounts.service.AccountService;
import com.amiti.financetracker.common.BadRequestException;
import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.domain.entity.TransactionEntity;
import com.amiti.financetracker.domain.model.TransactionType;
import com.amiti.financetracker.domain.repository.AccountRepository;
import com.amiti.financetracker.domain.repository.CategoryRepository;
import com.amiti.financetracker.domain.repository.TransactionRepository;
import com.amiti.financetracker.transactions.dto.TransactionDtos.TransactionRequest;
import com.amiti.financetracker.transactions.dto.TransactionDtos.TransactionResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final AccountService accountService;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository, CategoryRepository categoryRepository, AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.accountService = accountService;
    }

    public List<TransactionResponse> list(UUID userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDescCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    public TransactionResponse get(UUID userId, UUID transactionId) {
        return toResponse(transactionRepository.findByIdAndUserId(transactionId, userId).orElseThrow(() -> new NotFoundException("Transaction not found")));
    }

    @Transactional
    public TransactionResponse create(UUID userId, TransactionRequest request) {
        validate(request);
        TransactionEntity entity = new TransactionEntity();
        entity.setUserId(userId);
        apply(entity, request);
        TransactionEntity saved = transactionRepository.save(entity);
        if (!TransactionType.TRANSFER.equals(request.type())) {
            accountService.applyTransactionEffect(userId, request.accountId(), request.type().name(), request.amount(), false);
        }
        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID transactionId, TransactionRequest request) {
        validate(request);
        TransactionEntity entity = transactionRepository.findByIdAndUserId(transactionId, userId).orElseThrow(() -> new NotFoundException("Transaction not found"));
        if (!"TRANSFER".equals(entity.getType())) {
            accountService.applyTransactionEffect(userId, entity.getAccountId(), entity.getType(), entity.getAmount(), true);
        }
        apply(entity, request);
        TransactionEntity saved = transactionRepository.save(entity);
        if (!TransactionType.TRANSFER.equals(request.type())) {
            accountService.applyTransactionEffect(userId, request.accountId(), request.type().name(), request.amount(), false);
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID userId, UUID transactionId) {
        TransactionEntity entity = transactionRepository.findByIdAndUserId(transactionId, userId).orElseThrow(() -> new NotFoundException("Transaction not found"));
        if (!"TRANSFER".equals(entity.getType())) {
            accountService.applyTransactionEffect(userId, entity.getAccountId(), entity.getType(), entity.getAmount(), true);
        }
        transactionRepository.delete(entity);
    }

    public List<TransactionEntity> findForPeriod(UUID userId, LocalDate start, LocalDate end) {
        return transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(userId, start, end);
    }

    private void validate(TransactionRequest request) {
        if (request.accountId() == null) throw new BadRequestException("Account is required");
        accountRepository.findById(request.accountId()).orElseThrow(() -> new NotFoundException("Account not found"));
        if (request.type() != TransactionType.TRANSFER && request.categoryId() == null) throw new BadRequestException("Category is required for non-transfer transactions");
        if (request.categoryId() != null && categoryRepository.findById(request.categoryId()).isEmpty()) throw new NotFoundException("Category not found");
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
        return new TransactionResponse(entity.getId(), entity.getMerchant(), categoryName, accountName, TransactionType.valueOf(entity.getType()), entity.getAmount(), entity.getTransactionDate(), entity.getNote());
    }
}
