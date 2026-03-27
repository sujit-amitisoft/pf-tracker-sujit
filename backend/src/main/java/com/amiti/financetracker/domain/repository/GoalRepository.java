package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.GoalEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<GoalEntity, UUID> {
    List<GoalEntity> findByUserIdOrderByTargetDateAsc(UUID userId);

    List<GoalEntity> findByLinkedAccountIdInOrderByTargetDateAsc(Collection<UUID> accountIds);

    Optional<GoalEntity> findByIdAndUserId(UUID id, UUID userId);
}
