package com.amiti.financetracker.categories.service;

import com.amiti.financetracker.categories.dto.CategoryDtos.CategoryRequest;
import com.amiti.financetracker.categories.dto.CategoryDtos.CategoryResponse;
import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.domain.entity.CategoryEntity;
import com.amiti.financetracker.domain.model.CategoryType;
import com.amiti.financetracker.domain.repository.CategoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) { this.categoryRepository = categoryRepository; }

    public List<CategoryResponse> list(UUID userId) {
        return categoryRepository.findByUserIdOrUserIdIsNullOrderByNameAsc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(UUID userId, CategoryRequest request) {
        CategoryEntity entity = new CategoryEntity();
        entity.setUserId(userId);
        apply(entity, request);
        return toResponse(categoryRepository.save(entity));
    }

    @Transactional
    public CategoryResponse update(UUID userId, UUID categoryId, CategoryRequest request) {
        CategoryEntity entity = categoryRepository.findById(categoryId).filter(c -> userId.equals(c.getUserId()) || c.getUserId() == null).orElseThrow(() -> new NotFoundException("Category not found"));
        apply(entity, request);
        return toResponse(categoryRepository.save(entity));
    }

    @Transactional
    public void archive(UUID userId, UUID categoryId) {
        CategoryEntity entity = categoryRepository.findById(categoryId).filter(c -> userId.equals(c.getUserId()) || c.getUserId() == null).orElseThrow(() -> new NotFoundException("Category not found"));
        entity.setArchived(true);
        categoryRepository.save(entity);
    }

    private void apply(CategoryEntity entity, CategoryRequest request) {
        entity.setName(request.name());
        entity.setType(request.type().name());
        entity.setColor(request.color());
        entity.setIcon(request.icon());
        entity.setArchived(request.archived());
    }

    private CategoryResponse toResponse(CategoryEntity entity) {
        return new CategoryResponse(entity.getId(), entity.getName(), CategoryType.valueOf(entity.getType()), entity.getColor(), entity.getIcon(), entity.isArchived());
    }
}

