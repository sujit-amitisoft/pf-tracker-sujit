package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.RefreshTokenEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenAndRevokedFalse(String token);
    void deleteByUserIdAndExpiresAtBefore(UUID userId, LocalDateTime expiresAt);
}
