package com.amiti.financetracker.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_content_type")
    private String avatarContentType;

    // Map directly to Postgres `bytea` (avoid OID large-object mapping).
    @Column(name = "avatar_bytes", columnDefinition = "bytea")
    private byte[] avatarBytes;

    @Column(name = "avatar_updated_at")
    private LocalDateTime avatarUpdatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarContentType() { return avatarContentType; }
    public void setAvatarContentType(String avatarContentType) { this.avatarContentType = avatarContentType; }
    public byte[] getAvatarBytes() { return avatarBytes; }
    public void setAvatarBytes(byte[] avatarBytes) { this.avatarBytes = avatarBytes; }
    public LocalDateTime getAvatarUpdatedAt() { return avatarUpdatedAt; }
    public void setAvatarUpdatedAt(LocalDateTime avatarUpdatedAt) { this.avatarUpdatedAt = avatarUpdatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
