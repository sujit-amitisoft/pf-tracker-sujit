package com.amiti.financetracker.accounts.service;

import com.amiti.financetracker.accounts.dto.SharedAccountDtos.AccountInviteResponse;
import com.amiti.financetracker.accounts.dto.SharedAccountDtos.AccountMemberResponse;
import com.amiti.financetracker.accounts.dto.SharedAccountDtos.InviteMemberRequest;
import com.amiti.financetracker.accounts.dto.SharedAccountDtos.InvitePreviewResponse;
import com.amiti.financetracker.accounts.dto.SharedAccountDtos.UpdateMemberRoleRequest;
import com.amiti.financetracker.auth.dto.AuthDtos.MessageResponse;
import com.amiti.financetracker.common.BadRequestException;
import com.amiti.financetracker.common.NotFoundException;
import com.amiti.financetracker.common.mail.EmailService;
import com.amiti.financetracker.config.AppProperties;
import com.amiti.financetracker.domain.entity.AccountEntity;
import com.amiti.financetracker.domain.entity.AccountMemberEntity;
import com.amiti.financetracker.domain.entity.SharedAccountInviteEntity;
import com.amiti.financetracker.domain.entity.UserEntity;
import com.amiti.financetracker.domain.model.AccountMemberRole;
import com.amiti.financetracker.domain.repository.AccountMemberRepository;
import com.amiti.financetracker.domain.repository.AccountRepository;
import com.amiti.financetracker.domain.repository.SharedAccountInviteRepository;
import com.amiti.financetracker.domain.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SharedAccountService {
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final UserRepository userRepository;
    private final AccountActivityService accountActivityService;
    private final SharedAccountInviteRepository sharedAccountInviteRepository;
    private final EmailService emailService;
    private final AppProperties appProperties;

    public SharedAccountService(AccountRepository accountRepository, AccountMemberRepository accountMemberRepository, UserRepository userRepository, AccountActivityService accountActivityService, SharedAccountInviteRepository sharedAccountInviteRepository, EmailService emailService, AppProperties appProperties) {
        this.accountRepository = accountRepository;
        this.accountMemberRepository = accountMemberRepository;
        this.userRepository = userRepository;
        this.accountActivityService = accountActivityService;
        this.sharedAccountInviteRepository = sharedAccountInviteRepository;
        this.emailService = emailService;
        this.appProperties = appProperties;
    }

    public Set<UUID> accessibleAccountIds(UUID userId) {
        Set<UUID> ids = new LinkedHashSet<>(accountRepository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(AccountEntity::getId).toList());
        accountMemberRepository.findByUserId(userId).forEach(member -> ids.add(member.getAccountId()));
        return ids;
    }

    public void assertCanAccess(UUID userId, UUID accountId) {
        if (!canAccess(userId, accountId)) {
            throw new NotFoundException("Account not found");
        }
    }

    public void assertCanEdit(UUID userId, UUID accountId) {
        if (!canEdit(userId, accountId)) {
            throw new BadRequestException("You do not have permission to modify this account");
        }
    }

    public void assertOwner(UUID userId, UUID accountId) {
        AccountEntity account = accountRepository.findById(accountId).orElseThrow(() -> new NotFoundException("Account not found"));
        if (!userId.equals(account.getUserId())) {
            throw new BadRequestException("Only the account owner can manage members");
        }
    }

    public boolean canAccess(UUID userId, UUID accountId) {
        return accountRepository.findById(accountId).map(account -> userId.equals(account.getUserId())).orElse(false)
                || accountMemberRepository.findByAccountIdAndUserId(accountId, userId).isPresent();
    }

    public boolean canEdit(UUID userId, UUID accountId) {
        AccountEntity account = accountRepository.findById(accountId).orElse(null);
        if (account != null && userId.equals(account.getUserId())) {
            return true;
        }
        return accountMemberRepository.findByAccountIdAndUserId(accountId, userId)
                .map(member -> !AccountMemberRole.VIEWER.name().equals(member.getRole()))
                .orElse(false);
    }

    public List<AccountMemberResponse> listMembers(UUID userId, UUID accountId) {
        assertCanAccess(userId, accountId);
        AccountEntity account = accountRepository.findById(accountId).orElseThrow(() -> new NotFoundException("Account not found"));
        List<AccountMemberResponse> members = new ArrayList<>();
        UserEntity owner = userRepository.findById(account.getUserId()).orElseThrow(() -> new NotFoundException("Owner not found"));
        members.add(new AccountMemberResponse(owner.getId(), owner.getEmail(), owner.getDisplayName(), AccountMemberRole.OWNER, true, account.getCreatedAt()));
        accountMemberRepository.findByAccountIdOrderByCreatedAtAsc(accountId).forEach(member -> {
            UserEntity user = userRepository.findById(member.getUserId()).orElse(null);
            if (user == null) return;
            members.add(new AccountMemberResponse(user.getId(), user.getEmail(), user.getDisplayName(), AccountMemberRole.valueOf(member.getRole()), false, member.getCreatedAt()));
        });
        return members;
    }

    public List<AccountInviteResponse> listPendingInvites(UUID userId, UUID accountId) {
        assertOwner(userId, accountId);
        sharedAccountInviteRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        return sharedAccountInviteRepository.findByAccountIdAndAcceptedAtIsNullOrderByCreatedAtDesc(accountId).stream()
                .map(invite -> new AccountInviteResponse(invite.getId(), invite.getEmail(), AccountMemberRole.valueOf(invite.getRole()), invite.getExpiresAt(), invite.getCreatedAt(), invite.getAcceptedAt() != null))
                .toList();
    }

    public InvitePreviewResponse previewInvite(String token) {
        SharedAccountInviteEntity invite = validInvite(token);
        AccountEntity account = accountRepository.findById(invite.getAccountId()).orElseThrow(() -> new NotFoundException("Account not found"));
        UserEntity owner = userRepository.findById(account.getUserId()).orElseThrow(() -> new NotFoundException("Owner not found"));
        return new InvitePreviewResponse(account.getName(), invite.getEmail(), AccountMemberRole.valueOf(invite.getRole()), owner.getDisplayName(), invite.getExpiresAt(), invite.getAcceptedAt() != null);
    }

    @Transactional
    public MessageResponse invite(UUID userId, UUID accountId, InviteMemberRequest request) {
        assertOwner(userId, accountId);
        if (request.role() == AccountMemberRole.OWNER) {
            throw new BadRequestException("Invite role must be editor or viewer");
        }
        sharedAccountInviteRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        AccountEntity account = accountRepository.findById(accountId).orElseThrow(() -> new NotFoundException("Account not found"));
        UserEntity owner = userRepository.findById(account.getUserId()).orElseThrow(() -> new NotFoundException("Owner not found"));
        String email = request.email().trim().toLowerCase();
        if (email.equalsIgnoreCase(owner.getEmail())) {
            throw new BadRequestException("The account owner is already part of this account");
        }
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (accountMemberRepository.findByAccountIdAndUserId(accountId, existing.getId()).isPresent()) {
                throw new BadRequestException("This user is already a member of the account");
            }
        });
        sharedAccountInviteRepository.findByAccountIdAndEmailIgnoreCaseAndAcceptedAtIsNull(accountId, email).forEach(sharedAccountInviteRepository::delete);

        SharedAccountInviteEntity invite = new SharedAccountInviteEntity();
        invite.setAccountId(accountId);
        invite.setEmail(email);
        invite.setRole(request.role().name());
        invite.setToken(UUID.randomUUID().toString());
        invite.setInvitedBy(userId);
        invite.setExpiresAt(LocalDateTime.now().plusDays(7));
        sharedAccountInviteRepository.save(invite);

        String link = appProperties.frontendUrl() + "/auth?mode=invite&token=" + invite.getToken();
        String subject = "You were invited to a shared account";
        String body = "You were invited to collaborate on account '" + account.getName() + "' as " + request.role().name() + ".\n\n" +
                "Accept the invitation:\n" + link + "\n\n" +
                "This link expires in 7 days. If you do not yet have an account, sign up with this same email address first.";
        try {
            emailService.send(email, subject, body);
        } catch (RuntimeException ex) {
            throw new BadRequestException("Invitation email could not be sent. Check SMTP configuration.");
        }
        accountActivityService.log(accountId, userId, "MEMBER_INVITED", "INVITE", invite.getId(), "Sent shared account invite to " + email + " as " + request.role().name());
        return new MessageResponse("Invitation email sent.");
    }

    @Transactional
    public MessageResponse acceptInvite(UUID userId, String token) {
        SharedAccountInviteEntity invite = validInvite(token);
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.getEmail().equalsIgnoreCase(invite.getEmail())) {
            throw new BadRequestException("This invite was sent to a different email address");
        }
        AccountEntity account = accountRepository.findById(invite.getAccountId()).orElseThrow(() -> new NotFoundException("Account not found"));
        if (userId.equals(account.getUserId())) {
            throw new BadRequestException("The account owner cannot accept an invite to their own account");
        }
        AccountMemberEntity member = accountMemberRepository.findByAccountIdAndUserId(invite.getAccountId(), userId).orElseGet(AccountMemberEntity::new);
        member.setAccountId(invite.getAccountId());
        member.setUserId(userId);
        member.setRole(invite.getRole());
        member.setInvitedBy(invite.getInvitedBy());
        if (member.getCreatedAt() == null) {
            member.setCreatedAt(LocalDateTime.now());
        }
        accountMemberRepository.save(member);
        invite.setAcceptedAt(LocalDateTime.now());
        sharedAccountInviteRepository.save(invite);
        accountActivityService.log(invite.getAccountId(), userId, "INVITE_ACCEPTED", "INVITE", invite.getId(), user.getEmail() + " accepted the shared account invite");
        return new MessageResponse("Invitation accepted.");
    }

    @Transactional
    public List<AccountMemberResponse> updateMemberRole(UUID userId, UUID accountId, UUID memberUserId, UpdateMemberRoleRequest request) {
        assertOwner(userId, accountId);
        if (request.role() == AccountMemberRole.OWNER) {
            throw new BadRequestException("Owner role is reserved for the account owner");
        }
        AccountMemberEntity member = accountMemberRepository.findByAccountIdAndUserId(accountId, memberUserId).orElseThrow(() -> new NotFoundException("Member not found"));
        member.setRole(request.role().name());
        accountMemberRepository.save(member);
        UserEntity target = userRepository.findById(memberUserId).orElse(null);
        accountActivityService.log(accountId, userId, "ROLE_UPDATED", "MEMBER", memberUserId, "Changed role for " + (target != null ? target.getEmail() : "member") + " to " + request.role().name());
        return listMembers(userId, accountId);
    }

    private SharedAccountInviteEntity validInvite(String token) {
        return sharedAccountInviteRepository.findByToken(token)
                .filter(item -> item.getAcceptedAt() == null)
                .filter(item -> item.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired invite token"));
    }
}
