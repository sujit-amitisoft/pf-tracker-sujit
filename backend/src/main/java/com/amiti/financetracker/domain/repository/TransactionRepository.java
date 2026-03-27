package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.TransactionEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findByUserIdOrderByTransactionDateDescCreatedAtDesc(UUID userId);

    List<TransactionEntity> findByAccountIdInOrderByTransactionDateDescCreatedAtDesc(Collection<UUID> accountIds);

    List<TransactionEntity> findByAccountIdInAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(Collection<UUID> accountIds, LocalDate startDate, LocalDate endDate);

    Optional<TransactionEntity> findByIdAndUserId(UUID id, UUID userId);
}
