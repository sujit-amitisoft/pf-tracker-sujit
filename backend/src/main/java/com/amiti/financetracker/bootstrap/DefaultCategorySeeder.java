package com.amiti.financetracker.bootstrap;

import com.amiti.financetracker.domain.entity.CategoryEntity;
import com.amiti.financetracker.domain.repository.CategoryRepository;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class DefaultCategorySeeder {
    private static final Map<String, String> CATEGORY_COLORS = Map.ofEntries(
            Map.entry("Food", "#f97316"),
            Map.entry("Rent", "#2563eb"),
            Map.entry("Utilities", "#14b8a6"),
            Map.entry("Transport", "#0ea5e9"),
            Map.entry("Entertainment", "#8b5cf6"),
            Map.entry("Shopping", "#ec4899"),
            Map.entry("Health", "#22c55e"),
            Map.entry("Education", "#f59e0b"),
            Map.entry("Travel", "#06b6d4"),
            Map.entry("Subscriptions", "#6366f1"),
            Map.entry("Miscellaneous", "#64748b"),
            Map.entry("Salary", "#16a34a"),
            Map.entry("Freelance", "#0f766e"),
            Map.entry("Bonus", "#84cc16"),
            Map.entry("Investment", "#0891b2"),
            Map.entry("Gift", "#db2777"),
            Map.entry("Refund", "#10b981"),
            Map.entry("Other", "#6b7280")
    );

    @Bean
    @Order(0)
    CommandLineRunner seedDefaultCategories(CategoryRepository categoryRepository) {
        return args -> {
            seed(categoryRepository, "EXPENSE", List.of("Food", "Rent", "Utilities", "Transport", "Entertainment", "Shopping", "Health", "Education", "Travel", "Subscriptions", "Miscellaneous"));
            seed(categoryRepository, "INCOME", List.of("Salary", "Freelance", "Bonus", "Investment", "Gift", "Refund", "Other"));
        };
    }

    private void seed(CategoryRepository repository, String type, List<String> names) {
        var defaults = repository.findAll().stream()
                .filter(category -> category.getUserId() == null)
                .toList();

        names.forEach(name -> {
            CategoryEntity entity = defaults.stream()
                    .filter(category -> name.equals(category.getName()) && type.equals(category.getType()))
                    .findFirst()
                    .orElseGet(CategoryEntity::new);
            entity.setName(name);
            entity.setType(type);
            entity.setColor(CATEGORY_COLORS.getOrDefault(name, "#6e7993"));
            entity.setIcon(name.toLowerCase());
            repository.save(entity);
        });
    }
}
