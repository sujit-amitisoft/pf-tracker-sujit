package com.amiti.financetracker.transactions.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.transactions.dto.TransactionDtos.TransactionRequest;
import com.amiti.financetracker.transactions.dto.TransactionDtos.TransactionResponse;
import com.amiti.financetracker.transactions.service.TransactionService;
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
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) { this.transactionService = transactionService; }

    @GetMapping
    public List<TransactionResponse> list(Authentication authentication) { return transactionService.list(currentUserId(authentication)); }

    @GetMapping("/{id}")
    public TransactionResponse get(Authentication authentication, @PathVariable UUID id) { return transactionService.get(currentUserId(authentication), id); }

    @PostMapping
    public TransactionResponse create(Authentication authentication, @Valid @RequestBody TransactionRequest request) { return transactionService.create(currentUserId(authentication), request); }

    @PutMapping("/{id}")
    public TransactionResponse update(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody TransactionRequest request) { return transactionService.update(currentUserId(authentication), id, request); }

    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable UUID id) { transactionService.delete(currentUserId(authentication), id); }
}
