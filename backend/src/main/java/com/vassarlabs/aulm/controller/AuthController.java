package com.vassarlabs.aulm.controller;

import com.vassarlabs.aulm.dto.ForgotPasswordRequest;
import com.vassarlabs.aulm.dto.LoginRequest;
import com.vassarlabs.aulm.dto.LoginResponse;
import com.vassarlabs.aulm.dto.RegisterRequest;
import com.vassarlabs.aulm.dto.ResetPasswordRequest;
import com.vassarlabs.aulm.dto.UserResponse;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.service.AccessRequestService;
import com.vassarlabs.aulm.service.AuthService;
import com.vassarlabs.aulm.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccessRequestService accessRequestService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, AccessRequestService accessRequestService,
                           PasswordResetService passwordResetService) {
        this.authService = authService;
        this.accessRequestService = accessRequestService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = accessRequestService.register(request);
        return authService.issueTokenFor(user);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.from(user);
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
    }
}
