package com.amiti.financetracker.accounts.service;

import com.amiti.financetracker.accounts.dto.AccountDtos.AccountRequest;
import com.amiti.financetracker.accounts.dto.AccountDtos.AccountResponse;
import com.amiti.financetracker.accounts.dto.AccountDtos.TransferRequest;
import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.domain.entity.AccountEntity;
import com.amiti.financetracker.domain.model.AccountType;
import com.amiti.financetracker.domain.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<AccountResponse> list(UUID userId) {
        return accountRepository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(this::toResponse).toList();
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
        return toResponse(accountRepository.save(entity));
    }

    @Transactional
    public AccountResponse update(UUID userId, UUID accountId, AccountRequest request) {
        AccountEntity entity = accountRepository.findByIdAndUserId(accountId, userId).orElseThrow(() -> new NotFoundException("Account not found"));
        entity.setName(request.name());
        entity.setType(request.type().name());
        entity.setInstitutionName(request.institutionName());
        return toResponse(accountRepository.save(entity));
    }

    @Transactional
    public void transfer(UUID userId, TransferRequest request) {
        AccountEntity source = accountRepository.findByIdAndUserId(request.sourceAccountId(), userId).orElseThrow(() -> new NotFoundException("Source account not found"));
        AccountEntity destination = accountRepository.findByIdAndUserId(request.destinationAccountId(), userId).orElseThrow(() -> new NotFoundException("Destination account not found"));
        source.setCurrentBalance(source.getCurrentBalance().subtract(request.amount()));
        destination.setCurrentBalance(destination.getCurrentBalance().add(request.amount()));
        accountRepository.save(source);
        accountRepository.save(destination);
    }

    @Transactional
    public void applyTransactionEffect(UUID userId, UUID accountId, String type, BigDecimal amount, boolean reverse) {
        AccountEntity account = accountRepository.findByIdAndUserId(accountId, userId).orElseThrow(() -> new NotFoundException("Account not found"));
        BigDecimal delta = switch (type) {
            case "INCOME" -> amount;
            case "EXPENSE" -> amount.negate();
            default -> BigDecimal.ZERO;
        };
        if (reverse) delta = delta.negate();
        account.setCurrentBalance(account.getCurrentBalance().add(delta));
        accountRepository.save(account);
    }

    private AccountResponse toResponse(AccountEntity entity) {
        return new AccountResponse(entity.getId(), entity.getName(), AccountType.valueOf(entity.getType()), entity.getCurrentBalance(), entity.getInstitutionName(), entity.getLastUpdatedAt());
    }
}
