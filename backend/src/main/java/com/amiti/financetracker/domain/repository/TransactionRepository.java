package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.TransactionEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findByUserIdOrderByTransactionDateDescCreatedAtDesc(UUID userId);
    Optional<TransactionEntity> findByIdAndUserId(UUID id, UUID userId);
    List<TransactionEntity> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(UUID userId, LocalDate startDate, LocalDate endDate);
}
