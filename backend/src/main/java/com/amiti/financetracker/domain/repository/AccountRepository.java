package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.AccountEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    List<AccountEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);

    List<AccountEntity> findByIdInOrderByCreatedAtAsc(Collection<UUID> ids);

    Optional<AccountEntity> findByIdAndUserId(UUID id, UUID userId);
}
