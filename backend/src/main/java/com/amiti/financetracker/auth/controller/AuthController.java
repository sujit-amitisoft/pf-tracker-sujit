package com.amiti.financetracker.auth.controller;

import com.amiti.financetracker.auth.dto.AuthDtos.AuthResponse;
import com.amiti.financetracker.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.amiti.financetracker.auth.dto.AuthDtos.LoginRequest;
import com.amiti.financetracker.auth.dto.AuthDtos.MessageResponse;
import com.amiti.financetracker.auth.dto.AuthDtos.RefreshRequest;
import com.amiti.financetracker.auth.dto.AuthDtos.RegisterRequest;
import com.amiti.financetracker.auth.dto.AuthDtos.ResetPasswordRequest;
import com.amiti.financetracker.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}
