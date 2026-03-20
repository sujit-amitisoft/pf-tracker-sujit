package com.amiti.financetracker.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    @Column(name = "category_id")
    private UUID categoryId;
    @Column(name = "recurring_transaction_id")
    private UUID recurringTransactionId;
    @Column(nullable = false)
    private String type;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    private String merchant;
    private String note;
    @Column(name = "payment_method")
    private String paymentMethod;
    private String tags;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void prePersist(){ if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = LocalDateTime.now(); if (updatedAt == null) updatedAt = LocalDateTime.now(); }
    @PreUpdate void preUpdate(){ updatedAt = LocalDateTime.now(); }

    public UUID getId(){ return id; }
    public void setId(UUID id){ this.id=id; }
    public UUID getUserId(){ return userId; }
    public void setUserId(UUID userId){ this.userId=userId; }
    public UUID getAccountId(){ return accountId; }
    public void setAccountId(UUID accountId){ this.accountId=accountId; }
    public UUID getCategoryId(){ return categoryId; }
    public void setCategoryId(UUID categoryId){ this.categoryId=categoryId; }
    public UUID getRecurringTransactionId(){ return recurringTransactionId; }
    public void setRecurringTransactionId(UUID recurringTransactionId){ this.recurringTransactionId=recurringTransactionId; }
    public String getType(){ return type; }
    public void setType(String type){ this.type=type; }
    public BigDecimal getAmount(){ return amount; }
    public void setAmount(BigDecimal amount){ this.amount=amount; }
    public LocalDate getTransactionDate(){ return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate){ this.transactionDate=transactionDate; }
    public String getMerchant(){ return merchant; }
    public void setMerchant(String merchant){ this.merchant=merchant; }
    public String getNote(){ return note; }
    public void setNote(String note){ this.note=note; }
    public String getPaymentMethod(){ return paymentMethod; }
    public void setPaymentMethod(String paymentMethod){ this.paymentMethod=paymentMethod; }
    public String getTags(){ return tags; }
    public void setTags(String tags){ this.tags=tags; }
}
