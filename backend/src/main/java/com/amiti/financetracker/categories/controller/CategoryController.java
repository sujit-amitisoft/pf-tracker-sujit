package com.amiti.financetracker.categories.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.categories.dto.CategoryDtos.CategoryRequest;
import com.amiti.financetracker.categories.dto.CategoryDtos.CategoryResponse;
import com.amiti.financetracker.categories.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) { this.categoryService = categoryService; }

    @GetMapping
    public List<CategoryResponse> list(Authentication authentication) { return categoryService.list(currentUserId(authentication)); }

    @PostMapping
    public CategoryResponse create(Authentication authentication, @Valid @RequestBody CategoryRequest request) { return categoryService.create(currentUserId(authentication), request); }

    @PutMapping("/{id}")
    public CategoryResponse update(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody CategoryRequest request) { return categoryService.update(currentUserId(authentication), id, request); }

    @DeleteMapping("/{id}")
    public void archive(Authentication authentication, @PathVariable UUID id) { categoryService.archive(currentUserId(authentication), id); }
}
