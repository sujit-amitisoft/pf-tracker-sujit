package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.AccountActivityEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountActivityRepository extends JpaRepository<AccountActivityEntity, UUID> {
    List<AccountActivityEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
}