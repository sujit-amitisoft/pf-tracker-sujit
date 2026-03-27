package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.AccountMemberEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountMemberRepository extends JpaRepository<AccountMemberEntity, UUID> {
    List<AccountMemberEntity> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

    List<AccountMemberEntity> findByUserId(UUID userId);

    List<AccountMemberEntity> findByUserIdAndAccountIdIn(UUID userId, Collection<UUID> accountIds);

    Optional<AccountMemberEntity> findByAccountIdAndUserId(UUID accountId, UUID userId);
}