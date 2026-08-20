package com.vassarlabs.aulm.controller;

import com.vassarlabs.aulm.dto.AdminSummary;
import com.vassarlabs.aulm.dto.AccessCheckResponse;
import com.vassarlabs.aulm.dto.ForgotPasswordRequest;
import com.vassarlabs.aulm.dto.ForgotPasswordResponse;
import com.vassarlabs.aulm.dto.LoginRequest;
import com.vassarlabs.aulm.dto.LoginResponse;
import com.vassarlabs.aulm.dto.RegisterRequest;
import com.vassarlabs.aulm.dto.ResetPasswordRequest;
import com.vassarlabs.aulm.dto.UserResponse;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.service.AccessRequestService;
import com.vassarlabs.aulm.service.AuthService;
import com.vassarlabs.aulm.service.AccessCheckService;
import com.vassarlabs.aulm.service.PasswordResetService;
import com.vassarlabs.aulm.model.PermissionType;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AccessRequestService accessRequestService;
    private final PasswordResetService passwordResetService;
    private final AccessCheckService accessCheckService;

    public AuthController(AuthService authService, AccessRequestService accessRequestService,
                          PasswordResetService passwordResetService, AccessCheckService accessCheckService) {
        this.authService = authService;
        this.accessRequestService = accessRequestService;
        this.passwordResetService = passwordResetService;
        this.accessCheckService = accessCheckService;
    }

    /** Verifies username/project/password and returns a JWT + profile. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login username={} project={}", request.username(), request.projectName());
        return authService.login(request);
    }

    /** Self-service sign-up: creates the account plus a PENDING registration request, then logs the caller straight in. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register username={} project={}", request.username(), request.projectName());
        User user = accessRequestService.register(request);
        return authService.issueTokenFor(user);
    }

    /** Returns the currently authenticated user's own profile. */
    @GetMapping("/getMyProfile")
    public UserResponse getMyProfile(@AuthenticationPrincipal User user) {
        log.info("GET /api/auth/getMyProfile username={} project={}", user.getUsername(), user.getProjectName());
        return UserResponse.from(user);
    }

    /** Lists admins eligible to receive an access request for a project, for the "send request to" picker. */
    @GetMapping("/getAdmins")
    public List<AdminSummary> getAdmins(@RequestParam String projectName) {
        log.info("GET /api/auth/getAdmins project={}", projectName);
        return accessRequestService.getAdmins(projectName);
    }

    /**
     * Read-only check other systems call to ask "can this user do X in this project?".
     * Path intentionally left as /access-check (not renamed) — external systems outside this
     * codebase call it by this exact URL; renaming would break them silently.
     */
    @GetMapping("/access-check")
    public AccessCheckResponse checkAccess(@RequestParam String userName,
                                           @RequestParam String projectName,
                                           @RequestParam String permissionType) {
        PermissionType normalizedPermission = PermissionType.valueOf(permissionType.trim().toUpperCase(Locale.ROOT));
        log.info("GET /api/auth/access-check username={} project={} permission={}", userName, projectName, normalizedPermission);
        return new AccessCheckResponse(accessCheckService.checkAccess(userName, projectName, normalizedPermission));
    }

    /** Emails a time-limited password-reset link if the username/project/email match an account. */
    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("POST /api/auth/forgot-password username={} project={}", request.username(), request.projectName());
        return passwordResetService.forgotPassword(request);
    }

    /** Consumes a reset token from the emailed link and sets the new password. */
    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("POST /api/auth/reset-password token={}", request.token());
        passwordResetService.resetPassword(request);
    }
}
