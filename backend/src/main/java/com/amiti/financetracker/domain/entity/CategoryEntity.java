package com.amiti.financetracker.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class CategoryEntity {
    @Id
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String type;
    private String color;
    private String icon;
    @Column(name = "is_archived", nullable = false)
    private boolean isArchived;

    @PrePersist void prePersist(){ if (id == null) id = UUID.randomUUID(); }
    public UUID getId(){ return id; }
    public void setId(UUID id){ this.id = id; }
    public UUID getUserId(){ return userId; }
    public void setUserId(UUID userId){ this.userId = userId; }
    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
    public String getType(){ return type; }
    public void setType(String type){ this.type = type; }
    public String getColor(){ return color; }
    public void setColor(String color){ this.color = color; }
    public String getIcon(){ return icon; }
    public void setIcon(String icon){ this.icon = icon; }
    public boolean isArchived(){ return isArchived; }
    public void setArchived(boolean archived){ isArchived = archived; }
}
