package com.amiti.financetracker.goals.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.goals.dto.GoalDtos.GoalAmountRequest;
import com.amiti.financetracker.goals.dto.GoalDtos.GoalRequest;
import com.amiti.financetracker.goals.dto.GoalDtos.GoalResponse;
import com.amiti.financetracker.goals.service.GoalService;
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
@RequestMapping("/api/goals")
public class GoalController {
    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public List<GoalResponse> list(Authentication authentication) {
        return goalService.list(currentUserId(authentication));
    }

    @PostMapping
    public GoalResponse create(Authentication authentication, @Valid @RequestBody GoalRequest request) {
        return goalService.create(currentUserId(authentication), request);
    }

    @PutMapping("/{id}")
    public GoalResponse update(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody GoalRequest request) {
        return goalService.update(currentUserId(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        goalService.delete(currentUserId(authentication), id);
    }

    @PostMapping("/{id}/contribute")
    public GoalResponse contribute(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody GoalAmountRequest request) {
        return goalService.contribute(currentUserId(authentication), id, request);
    }

    @PostMapping("/{id}/withdraw")
    public GoalResponse withdraw(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody GoalAmountRequest request) {
        return goalService.withdraw(currentUserId(authentication), id, request);
    }
}
