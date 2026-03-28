package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.RecurringTransactionEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransactionEntity, UUID> {
    List<RecurringTransactionEntity> findByUserIdOrderByNextRunDateAsc(UUID userId);
    List<RecurringTransactionEntity> findByAccountIdInOrderByNextRunDateAsc(Iterable<UUID> accountIds);
    Optional<RecurringTransactionEntity> findByIdAndUserId(UUID id, UUID userId);
    List<RecurringTransactionEntity> findByPausedFalseAndAutoCreateTransactionTrueAndNextRunDateLessThanEqual(LocalDate date);
}