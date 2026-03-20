package com.amiti.financetracker.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budgets")
public class BudgetEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "category_id", nullable = false) private UUID categoryId;
    @Column(nullable = false) private int month;
    @Column(nullable = false) private int year;
    @Column(nullable = false) private BigDecimal amount;
    @Column(name = "alert_threshold_percent") private Integer alertThresholdPercent;
    @PrePersist void prePersist(){ if(id==null) id=UUID.randomUUID(); if(alertThresholdPercent==null) alertThresholdPercent=80; }
    public UUID getId(){ return id; } public void setId(UUID id){ this.id=id; }
    public UUID getUserId(){ return userId; } public void setUserId(UUID userId){ this.userId=userId; }
    public UUID getCategoryId(){ return categoryId; } public void setCategoryId(UUID categoryId){ this.categoryId=categoryId; }
    public int getMonth(){ return month; } public void setMonth(int month){ this.month=month; }
    public int getYear(){ return year; } public void setYear(int year){ this.year=year; }
    public BigDecimal getAmount(){ return amount; } public void setAmount(BigDecimal amount){ this.amount=amount; }
    public Integer getAlertThresholdPercent(){ return alertThresholdPercent; } public void setAlertThresholdPercent(Integer v){ this.alertThresholdPercent=v; }
}
