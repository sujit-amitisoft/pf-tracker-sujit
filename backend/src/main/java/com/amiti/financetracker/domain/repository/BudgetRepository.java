package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.BudgetEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<BudgetEntity, UUID> {
    List<BudgetEntity> findByMonthAndYear(int month, int year);
    List<BudgetEntity> findByUserIdAndMonthAndYear(UUID userId, int month, int year);
}