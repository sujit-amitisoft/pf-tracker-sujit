package com.amiti.financetracker.rules.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.rules.dto.RuleDtos.RuleRequest;
import com.amiti.financetracker.rules.dto.RuleDtos.RuleResponse;
import com.amiti.financetracker.rules.service.RuleService;
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
@RequestMapping("/api/rules")
public class RuleController {
    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public List<RuleResponse> list(Authentication authentication) {
        return ruleService.list(currentUserId(authentication));
    }

    @PostMapping
    public RuleResponse create(Authentication authentication, @Valid @RequestBody RuleRequest request) {
        return ruleService.create(currentUserId(authentication), request);
    }

    @PutMapping("/{id}")
    public RuleResponse update(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody RuleRequest request) {
        return ruleService.update(currentUserId(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        ruleService.delete(currentUserId(authentication), id);
    }
}