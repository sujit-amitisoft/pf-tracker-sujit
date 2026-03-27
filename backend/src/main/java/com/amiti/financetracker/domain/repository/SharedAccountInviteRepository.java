package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.SharedAccountInviteEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedAccountInviteRepository extends JpaRepository<SharedAccountInviteEntity, UUID> {
    Optional<SharedAccountInviteEntity> findByToken(String token);
    List<SharedAccountInviteEntity> findByAccountIdAndAcceptedAtIsNullOrderByCreatedAtDesc(UUID accountId);
    List<SharedAccountInviteEntity> findByAccountIdAndEmailIgnoreCaseAndAcceptedAtIsNull(UUID accountId, String email);
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}