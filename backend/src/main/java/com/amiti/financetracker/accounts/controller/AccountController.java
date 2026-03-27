package com.amiti.financetracker.accounts.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.accounts.dto.AccountDtos.AccountRequest;
import com.amiti.financetracker.accounts.dto.AccountDtos.AccountResponse;
import com.amiti.financetracker.accounts.dto.AccountDtos.TransferRequest;
import com.amiti.financetracker.accounts.dto.SharedAccountDtos.AccountActivityResponse;
import com.amiti.financetracker.accounts.dto.SharedAccountDtos.AccountInviteResponse;
import com.amiti.financetracker.accounts.dto.SharedAccountDtos.AccountMemberResponse;
import com.amiti.financetracker.accounts.dto.SharedAccountDtos.InviteMemberRequest;
import com.amiti.financetracker.accounts.dto.SharedAccountDtos.InvitePreviewResponse;
import com.amiti.financetracker.accounts.dto.SharedAccountDtos.UpdateMemberRoleRequest;
import com.amiti.financetracker.accounts.service.AccountActivityService;
import com.amiti.financetracker.accounts.service.AccountService;
import com.amiti.financetracker.accounts.service.SharedAccountService;
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
    private final SharedAccountService sharedAccountService;
    private final AccountActivityService accountActivityService;

    public AccountController(AccountService accountService, SharedAccountService sharedAccountService, AccountActivityService accountActivityService) {
        this.accountService = accountService;
        this.sharedAccountService = sharedAccountService;
        this.accountActivityService = accountActivityService;
    }

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

    @GetMapping("/{id}/members")
    public List<AccountMemberResponse> members(Authentication authentication, @PathVariable UUID id) { return sharedAccountService.listMembers(currentUserId(authentication), id); }

    @GetMapping("/{id}/activity")
    public List<AccountActivityResponse> activity(Authentication authentication, @PathVariable UUID id) { return accountActivityService.list(currentUserId(authentication), id); }

    @GetMapping("/{id}/invites")
    public List<AccountInviteResponse> invites(Authentication authentication, @PathVariable UUID id) { return sharedAccountService.listPendingInvites(currentUserId(authentication), id); }

    @GetMapping("/invites/{token}")
    public InvitePreviewResponse previewInvite(@PathVariable String token) { return sharedAccountService.previewInvite(token); }

    @PostMapping("/invites/{token}/accept")
    public MessageResponse acceptInvite(Authentication authentication, @PathVariable String token) { return sharedAccountService.acceptInvite(currentUserId(authentication), token); }

    @PostMapping("/{id}/invite")
    public MessageResponse invite(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody InviteMemberRequest request) { return sharedAccountService.invite(currentUserId(authentication), id, request); }

    @PutMapping("/{id}/members/{userId}")
    public List<AccountMemberResponse> updateMemberRole(Authentication authentication, @PathVariable UUID id, @PathVariable UUID userId, @Valid @RequestBody UpdateMemberRoleRequest request) {
        return sharedAccountService.updateMemberRole(currentUserId(authentication), id, userId, request);
    }
}