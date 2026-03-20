package com.amiti.financetracker.auth.service;

import com.amiti.financetracker.auth.dto.AuthDtos.AuthResponse;
import com.amiti.financetracker.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.amiti.financetracker.auth.dto.AuthDtos.LoginRequest;
import com.amiti.financetracker.auth.dto.AuthDtos.MessageResponse;
import com.amiti.financetracker.auth.dto.AuthDtos.RefreshRequest;
import com.amiti.financetracker.auth.dto.AuthDtos.RegisterRequest;
import com.amiti.financetracker.auth.dto.AuthDtos.ResetPasswordRequest;
import com.amiti.financetracker.common.BadRequestException;
import com.amiti.financetracker.config.AppProperties;
import com.amiti.financetracker.domain.entity.RefreshTokenEntity;
import com.amiti.financetracker.domain.entity.UserEntity;
import com.amiti.financetracker.domain.repository.RefreshTokenRepository;
import com.amiti.financetracker.domain.repository.UserRepository;
import com.amiti.financetracker.security.JwtService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AppProperties appProperties;
    private final Map<String, UUID> passwordResetTokens = new ConcurrentHashMap<>();

    public AuthService(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            AppProperties appProperties
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            throw new BadRequestException("Email already exists");
        });
        UserEntity user = new UserEntity();
        user.setEmail(request.email().trim().toLowerCase());
        user.setDisplayName(request.displayName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        UserEntity saved = userRepository.save(user);
        return authResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Incorrect login details. Please check your email and password."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Incorrect login details. Please check your email and password.");
        }
        return authResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshTokenEntity token = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
                .filter(item -> item.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        UserEntity user = userRepository.findById(token.getUserId()).orElseThrow(() -> new BadRequestException("User not found"));
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return authResponse(user);
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("No account found for that email"));
        String token = UUID.randomUUID().toString();
        passwordResetTokens.put(token, user.getId());
        return new MessageResponse("Reset token generated for development: " + token);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        UUID userId = passwordResetTokens.remove(request.token());
        if (userId == null) {
            throw new BadRequestException("Invalid or expired reset token");
        }
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return new MessageResponse("Password reset complete.");
    }

    private AuthResponse authResponse(UserEntity user) {
        JwtService.AccessToken accessToken = jwtService.issueAccessToken(user);
        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(appProperties.jwt().refreshExpiration() / 1000));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
        refreshTokenRepository.deleteByUserIdAndExpiresAtBefore(user.getId(), LocalDateTime.now());
        return new AuthResponse(user.getId(), user.getDisplayName(), user.getEmail(), accessToken.value(), refreshToken.getToken(), accessToken.expiresAt());
    }
}
