package com.amiti.financetracker.domain.repository;

import com.amiti.financetracker.domain.entity.CategoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    List<CategoryEntity> findByUserIdOrUserIdIsNullOrderByNameAsc(UUID userId);
}
