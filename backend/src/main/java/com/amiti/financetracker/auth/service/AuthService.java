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
import com.amiti.financetracker.common.mail.EmailService;
import com.amiti.financetracker.domain.entity.RefreshTokenEntity;
import com.amiti.financetracker.domain.entity.PasswordResetTokenEntity;
import com.amiti.financetracker.domain.entity.UserEntity;
import com.amiti.financetracker.domain.repository.RefreshTokenRepository;
import com.amiti.financetracker.domain.repository.PasswordResetTokenRepository;
import com.amiti.financetracker.domain.repository.UserRepository;
import com.amiti.financetracker.security.JwtService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    public AuthService(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            JwtService jwtService,
            AppProperties appProperties
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
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
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email().trim())
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
        // Always return the same message to avoid account enumeration.
        userRepository.findByEmailIgnoreCase(request.email().trim()).ifPresent(user -> {
            String tokenValue = UUID.randomUUID().toString();
            PasswordResetTokenEntity token = new PasswordResetTokenEntity();
            token.setUserId(user.getId());
            token.setToken(tokenValue);
            token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            token.setUsedAt(null);
            passwordResetTokenRepository.save(token);

            String link = appProperties.frontendUrl() + "/auth?mode=reset&token=" + tokenValue;
            String body = "You requested a password reset for Personal Finance Tracker.\n\n" +
                    "Reset link (valid for 30 minutes):\n" + link + "\n\n" +
                    "If you didn't request this, you can ignore this email.";
            try {
                emailService.send(user.getEmail(), "Reset your password", body);
            } catch (RuntimeException ex) {
                // Don't leak details to the client; logs will capture send failures.
            }
        });
        return new MessageResponse("If an account exists for that email, we sent a password reset link.");
    }

        @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetTokenEntity token = passwordResetTokenRepository.findByToken(request.token())
                .filter(item -> item.getUsedAt() == null)
                .filter(item -> item.getExpiresAt() != null && item.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        UserEntity user = userRepository.findById(token.getUserId()).orElseThrow(() -> new BadRequestException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
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



