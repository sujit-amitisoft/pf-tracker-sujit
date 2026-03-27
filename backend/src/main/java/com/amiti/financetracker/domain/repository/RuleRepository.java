package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.RuleEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<RuleEntity, UUID> {
    List<RuleEntity> findByUserIdOrderByPriorityAscCreatedAtAsc(UUID userId);

    List<RuleEntity> findByUserIdAndActiveTrueOrderByPriorityAscCreatedAtAsc(UUID userId);
}