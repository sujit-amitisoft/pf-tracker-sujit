package com.amiti.financetracker.budgets.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.budgets.dto.BudgetDtos.BudgetRequest;
import com.amiti.financetracker.budgets.dto.BudgetDtos.BudgetResponse;
import com.amiti.financetracker.budgets.service.BudgetService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) { this.budgetService = budgetService; }

    @GetMapping
    public List<BudgetResponse> list(Authentication authentication, @RequestParam int month, @RequestParam int year) { return budgetService.list(currentUserId(authentication), month, year); }

    @PostMapping
    public BudgetResponse create(Authentication authentication, @Valid @RequestBody BudgetRequest request) { return budgetService.create(currentUserId(authentication), request); }

    @PutMapping("/{id}")
    public BudgetResponse update(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody BudgetRequest request) { return budgetService.update(currentUserId(authentication), id, request); }

    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable UUID id) { budgetService.delete(currentUserId(authentication), id); }
}
