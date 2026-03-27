package com.amiti.financetracker.accounts.service;

import com.amiti.financetracker.accounts.dto.SharedAccountDtos.AccountActivityResponse;
import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.domain.entity.AccountActivityEntity;
import com.amiti.financetracker.domain.entity.AccountEntity;
import com.amiti.financetracker.domain.entity.UserEntity;
import com.amiti.financetracker.domain.repository.AccountActivityRepository;
import com.amiti.financetracker.domain.repository.AccountMemberRepository;
import com.amiti.financetracker.domain.repository.AccountRepository;
import com.amiti.financetracker.domain.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountActivityService {
    private final AccountActivityRepository accountActivityRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final UserRepository userRepository;

    public AccountActivityService(AccountActivityRepository accountActivityRepository, AccountRepository accountRepository, AccountMemberRepository accountMemberRepository, UserRepository userRepository) {
        this.accountActivityRepository = accountActivityRepository;
        this.accountRepository = accountRepository;
        this.accountMemberRepository = accountMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void log(UUID accountId, UUID actorUserId, String activityType, String subjectType, UUID subjectId, String summary) {
        if (accountId == null || actorUserId == null || summary == null || summary.isBlank()) {
            return;
        }
        AccountActivityEntity entity = new AccountActivityEntity();
        entity.setAccountId(accountId);
        entity.setActorUserId(actorUserId);
        entity.setActivityType(activityType);
        entity.setSubjectType(subjectType);
        entity.setSubjectId(subjectId);
        entity.setSummary(summary);
        accountActivityRepository.save(entity);
    }

    public List<AccountActivityResponse> list(UUID userId, UUID accountId) {
        assertCanAccess(userId, accountId);
        return accountActivityRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream().map(this::toResponse).toList();
    }

    private void assertCanAccess(UUID userId, UUID accountId) {
        AccountEntity account = accountRepository.findById(accountId).orElseThrow(() -> new NotFoundException("Account not found"));
        boolean canAccess = userId.equals(account.getUserId()) || accountMemberRepository.findByAccountIdAndUserId(accountId, userId).isPresent();
        if (!canAccess) {
            throw new NotFoundException("Account not found");
        }
    }

    private AccountActivityResponse toResponse(AccountActivityEntity entity) {
        UserEntity actor = userRepository.findById(entity.getActorUserId()).orElse(null);
        return new AccountActivityResponse(
                entity.getId(),
                entity.getActivityType(),
                entity.getSubjectType(),
                entity.getSubjectId(),
                actor != null ? actor.getDisplayName() : "Unknown user",
                actor != null ? actor.getEmail() : null,
                entity.getSummary(),
                entity.getCreatedAt()
        );
    }
}