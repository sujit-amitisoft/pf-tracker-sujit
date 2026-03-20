package com.amiti.financetracker.recurring.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.recurring.dto.RecurringDtos.RecurringRequest;
import com.amiti.financetracker.recurring.dto.RecurringDtos.RecurringResponse;
import com.amiti.financetracker.recurring.service.RecurringService;
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
@RequestMapping("/api/recurring")
public class RecurringController {
    private final RecurringService recurringService;

    public RecurringController(RecurringService recurringService) { this.recurringService = recurringService; }

    @GetMapping
    public List<RecurringResponse> list(Authentication authentication) { return recurringService.list(currentUserId(authentication)); }

    @PostMapping
    public RecurringResponse create(Authentication authentication, @Valid @RequestBody RecurringRequest request) { return recurringService.create(currentUserId(authentication), request); }

    @PutMapping("/{id}")
    public RecurringResponse update(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody RecurringRequest request) { return recurringService.update(currentUserId(authentication), id, request); }

    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable UUID id) { recurringService.delete(currentUserId(authentication), id); }
}
