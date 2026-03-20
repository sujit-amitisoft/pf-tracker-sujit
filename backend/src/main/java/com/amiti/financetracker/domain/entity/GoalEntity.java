package com.amiti.financetracker.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goals")
public class GoalEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private String name;
    @Column(name = "target_amount", nullable = false) private BigDecimal targetAmount;
    @Column(name = "current_amount", nullable = false) private BigDecimal currentAmount;
    @Column(name = "target_date") private LocalDate targetDate;
    @Column(name = "linked_account_id") private UUID linkedAccountId;
    @Column private String icon;
    @Column private String color;
    @Column(nullable = false) private String status;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (currentAmount == null) currentAmount = BigDecimal.ZERO;
        if (status == null) status = "ACTIVE";
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public UUID getLinkedAccountId() { return linkedAccountId; }
    public void setLinkedAccountId(UUID linkedAccountId) { this.linkedAccountId = linkedAccountId; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
