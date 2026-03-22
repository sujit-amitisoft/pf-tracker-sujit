package com.amiti.financetracker.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class UserDtos {
    public record MeResponse(UUID id, String email, String displayName, boolean hasAvatar) {}

    public record UpdateMeRequest(
            @NotBlank @Size(max = 120) String displayName
    ) {}
}
