package com.amiti.financetracker.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.amiti.financetracker.auth.dto.AuthDtos.RegisterRequest;
import com.amiti.financetracker.config.AppProperties;
import com.amiti.financetracker.domain.entity.RefreshTokenEntity;
import com.amiti.financetracker.domain.entity.UserEntity;
import com.amiti.financetracker.domain.repository.RefreshTokenRepository;
import com.amiti.financetracker.domain.repository.UserRepository;
import com.amiti.financetracker.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {

    @Test
    void registerReturnsTokensAndPersistsUser() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        when(userRepository.findByEmailIgnoreCase("demo@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppProperties properties = new AppProperties(
                "http://localhost:5173",
                "http://localhost:5173",
                new AppProperties.Jwt("change-me-change-me-change-me-change-me", 900000, 2592000000L),
                new AppProperties.Scheduler(true)
        );
        JwtService jwtService = new JwtService(properties);

        AuthService service = new AuthService(
                new BCryptPasswordEncoder(),
                userRepository,
                refreshTokenRepository,
                jwtService,
                properties
        );

        var response = service.register(new RegisterRequest("demo@example.com", "Password1", "Demo"));

        assertNotNull(response.userId());
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertNotNull(response.expiresAt());
        assertEquals("demo@example.com", response.email());
    }
}
