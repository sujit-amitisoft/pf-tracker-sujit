package com.amiti.financetracker.accounts.dto;

import com.amiti.financetracker.domain.model.AccountMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public class SharedAccountDtos {
    private SharedAccountDtos() {
    }

    public record AccountMemberResponse(
            UUID userId,
            String email,
            String displayName,
            AccountMemberRole role,
            boolean owner,
            LocalDateTime addedAt
    ) {
    }

    public record AccountActivityResponse(
            UUID id,
            String activityType,
            String subjectType,
            UUID subjectId,
            String actorName,
            String actorEmail,
            String summary,
            LocalDateTime createdAt
    ) {
    }

    public record AccountInviteResponse(
            UUID id,
            String email,
            AccountMemberRole role,
            LocalDateTime expiresAt,
            LocalDateTime createdAt,
            boolean accepted
    ) {
    }

    public record InvitePreviewResponse(
            String accountName,
            String invitedEmail,
            AccountMemberRole role,
            String ownerName,
            LocalDateTime expiresAt,
            boolean accepted
    ) {
    }

    public record InviteMemberRequest(
            @Email @NotBlank String email,
            @NotNull AccountMemberRole role
    ) {
    }

    public record UpdateMemberRoleRequest(
            @NotNull AccountMemberRole role
    ) {
    }
}