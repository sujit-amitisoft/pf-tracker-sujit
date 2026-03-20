package com.amiti.financetracker.accounts.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.accounts.dto.AccountDtos.AccountRequest;
import com.amiti.financetracker.accounts.dto.AccountDtos.AccountResponse;
import com.amiti.financetracker.accounts.dto.AccountDtos.TransferRequest;
import com.amiti.financetracker.accounts.service.AccountService;
import com.amiti.financetracker.auth.dto.AuthDtos.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) { this.accountService = accountService; }

    @GetMapping
    public List<AccountResponse> list(Authentication authentication) { return accountService.list(currentUserId(authentication)); }

    @PostMapping
    public AccountResponse create(Authentication authentication, @Valid @RequestBody AccountRequest request) { return accountService.create(currentUserId(authentication), request); }

    @PutMapping("/{id}")
    public AccountResponse update(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody AccountRequest request) { return accountService.update(currentUserId(authentication), id, request); }

    @PostMapping("/transfer")
    public MessageResponse transfer(Authentication authentication, @Valid @RequestBody TransferRequest request) {
        accountService.transfer(currentUserId(authentication), request);
        return new MessageResponse("Transfer completed");
    }
}
