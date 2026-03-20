package com.amiti.financetracker.categories.dto;

import com.amiti.financetracker.domain.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CategoryDtos {
    private CategoryDtos() {
    }

    public record CategoryResponse(
            UUID id,
            String name,
            CategoryType type,
            String color,
            String icon,
            boolean archived
    ) {
    }

    public record CategoryRequest(
            @NotBlank String name,
            @NotNull CategoryType type,
            String color,
            String icon,
            boolean archived
    ) {
    }
}
