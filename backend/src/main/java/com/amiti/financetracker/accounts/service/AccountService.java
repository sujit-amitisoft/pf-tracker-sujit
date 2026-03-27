package com.amiti.financetracker.accounts.service;

import com.amiti.financetracker.accounts.dto.AccountDtos.AccountRequest;
import com.amiti.financetracker.accounts.dto.AccountDtos.AccountResponse;
import com.amiti.financetracker.accounts.dto.AccountDtos.TransferRequest;
import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.domain.entity.AccountEntity;
import com.amiti.financetracker.domain.model.AccountType;
import com.amiti.financetracker.domain.repository.AccountMemberRepository;
import com.amiti.financetracker.domain.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final SharedAccountService sharedAccountService;
    private final AccountActivityService accountActivityService;

    public AccountService(AccountRepository accountRepository, AccountMemberRepository accountMemberRepository, SharedAccountService sharedAccountService, AccountActivityService accountActivityService) {
        this.accountRepository = accountRepository;
        this.accountMemberRepository = accountMemberRepository;
        this.sharedAccountService = sharedAccountService;
        this.accountActivityService = accountActivityService;
    }

    public List<AccountResponse> list(UUID userId) {
        Set<UUID> accessible = sharedAccountService.accessibleAccountIds(userId);
        if (accessible.isEmpty()) {
            return List.of();
        }
        return accountRepository.findByIdInOrderByCreatedAtAsc(accessible).stream().map(account -> toResponse(userId, account)).toList();
    }

    @Transactional
    public AccountResponse create(UUID userId, AccountRequest request) {
        AccountEntity entity = new AccountEntity();
        entity.setUserId(userId);
        entity.setName(request.name());
        entity.setType(request.type().name());
        entity.setInstitutionName(request.institutionName());
        entity.setOpeningBalance(request.openingBalance() == null ? BigDecimal.ZERO : request.openingBalance());
        entity.setCurrentBalance(entity.getOpeningBalance());
        AccountEntity saved = accountRepository.save(entity);
        accountActivityService.log(saved.getId(), userId, "ACCOUNT_CREATED", "ACCOUNT", saved.getId(), "Created account " + saved.getName());
        return toResponse(userId, saved);
    }

    @Transactional
    public AccountResponse update(UUID userId, UUID accountId, AccountRequest request) {
        sharedAccountService.assertCanEdit(userId, accountId);
        AccountEntity entity = accountRepository.findById(accountId).orElseThrow(() -> new NotFoundException("Account not found"));
        entity.setName(request.name());
        entity.setType(request.type().name());
        entity.setInstitutionName(request.institutionName());
        AccountEntity saved = accountRepository.save(entity);
        accountActivityService.log(saved.getId(), userId, "ACCOUNT_UPDATED", "ACCOUNT", saved.getId(), "Updated account details for " + saved.getName());
        return toResponse(userId, saved);
    }

    @Transactional
    public void transfer(UUID userId, TransferRequest request) {
        sharedAccountService.assertCanEdit(userId, request.sourceAccountId());
        sharedAccountService.assertCanEdit(userId, request.destinationAccountId());
        AccountEntity source = accountRepository.findById(request.sourceAccountId()).orElseThrow(() -> new NotFoundException("Source account not found"));
        AccountEntity destination = accountRepository.findById(request.destinationAccountId()).orElseThrow(() -> new NotFoundException("Destination account not found"));
        source.setCurrentBalance(source.getCurrentBalance().subtract(request.amount()));
        destination.setCurrentBalance(destination.getCurrentBalance().add(request.amount()));
        accountRepository.save(source);
        accountRepository.save(destination);
        accountActivityService.log(source.getId(), userId, "TRANSFER_OUT", "TRANSFER", null, "Transferred $" + request.amount() + " to " + destination.getName());
        accountActivityService.log(destination.getId(), userId, "TRANSFER_IN", "TRANSFER", null, "Received $" + request.amount() + " from " + source.getName());
    }

    @Transactional
    public void applyTransactionEffect(UUID userId, UUID accountId, String type, BigDecimal amount, boolean reverse) {
        sharedAccountService.assertCanEdit(userId, accountId);
        AccountEntity account = accountRepository.findById(accountId).orElseThrow(() -> new NotFoundException("Account not found"));
        BigDecimal delta = switch (type) {
            case "INCOME" -> amount;
            case "EXPENSE" -> amount.negate();
            default -> BigDecimal.ZERO;
        };
        if (reverse) delta = delta.negate();
        account.setCurrentBalance(account.getCurrentBalance().add(delta));
        accountRepository.save(account);
    }

    private AccountResponse toResponse(UUID currentUserId, AccountEntity entity) {
        boolean shared = !currentUserId.equals(entity.getUserId());
        String accessRole = currentUserId.equals(entity.getUserId())
                ? "OWNER"
                : accountMemberRepository.findByAccountIdAndUserId(entity.getId(), currentUserId).map(member -> member.getRole()).orElse("VIEWER");
        int memberCount = accountMemberRepository.findByAccountIdOrderByCreatedAtAsc(entity.getId()).size() + 1;
        return new AccountResponse(entity.getId(), entity.getName(), AccountType.valueOf(entity.getType()), entity.getCurrentBalance(), entity.getInstitutionName(), entity.getLastUpdatedAt(), shared, accessRole, memberCount);
    }
}