package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.AccountEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    List<AccountEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);
    Optional<AccountEntity> findByIdAndUserId(UUID id, UUID userId);
}
