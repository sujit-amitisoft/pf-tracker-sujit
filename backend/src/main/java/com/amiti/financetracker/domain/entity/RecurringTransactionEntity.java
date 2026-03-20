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
@Table(name = "recurring_transactions")
public class RecurringTransactionEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private BigDecimal amount;
    @Column(name = "category_id") private UUID categoryId;
    @Column(name = "account_id") private UUID accountId;
    @Column(nullable = false) private String frequency;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "next_run_date", nullable = false) private LocalDate nextRunDate;
    @Column(name = "auto_create_transaction", nullable = false) private boolean autoCreateTransaction;
    @Column(nullable = false) private boolean paused;
    @PrePersist void prePersist(){ if(id==null) id=UUID.randomUUID(); }
    public UUID getId(){ return id; } public void setId(UUID id){ this.id=id; }
    public UUID getUserId(){ return userId; } public void setUserId(UUID userId){ this.userId=userId; }
    public String getTitle(){ return title; } public void setTitle(String title){ this.title=title; }
    public String getType(){ return type; } public void setType(String type){ this.type=type; }
    public BigDecimal getAmount(){ return amount; } public void setAmount(BigDecimal amount){ this.amount=amount; }
    public UUID getCategoryId(){ return categoryId; } public void setCategoryId(UUID categoryId){ this.categoryId=categoryId; }
    public UUID getAccountId(){ return accountId; } public void setAccountId(UUID accountId){ this.accountId=accountId; }
    public String getFrequency(){ return frequency; } public void setFrequency(String frequency){ this.frequency=frequency; }
    public LocalDate getStartDate(){ return startDate; } public void setStartDate(LocalDate startDate){ this.startDate=startDate; }
    public LocalDate getEndDate(){ return endDate; } public void setEndDate(LocalDate endDate){ this.endDate=endDate; }
    public LocalDate getNextRunDate(){ return nextRunDate; } public void setNextRunDate(LocalDate nextRunDate){ this.nextRunDate=nextRunDate; }
    public boolean isAutoCreateTransaction(){ return autoCreateTransaction; } public void setAutoCreateTransaction(boolean autoCreateTransaction){ this.autoCreateTransaction=autoCreateTransaction; }
    public boolean isPaused(){ return paused; } public void setPaused(boolean paused){ this.paused=paused; }
}
