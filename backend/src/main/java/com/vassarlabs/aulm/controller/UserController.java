package com.vassarlabs.aulm.controller;

import com.vassarlabs.aulm.dto.CreateUserRequest;
import com.vassarlabs.aulm.dto.RenewLicenseRequest;
import com.vassarlabs.aulm.dto.UpdateUserRequest;
import com.vassarlabs.aulm.dto.UserResponse;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> listUsers(@AuthenticationPrincipal User admin) {
        log.info("GET /api/users username={} project={}", admin.getUsername(), admin.getProjectName());
        return userService.listUsers(admin).stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable("id") Long id) {
        log.info("GET /api/users/{}", id);
        return UserResponse.from(userService.getUser(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /api/users username={} project={}", request.username(), request.projectName());
        User user = userService.createUser(request);
        return UserResponse.from(user);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable("id") Long id, @Valid @RequestBody UpdateUserRequest request) {
        log.info("PUT /api/users/{}", id);
        return UserResponse.from(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("id") Long id) {
        log.info("DELETE /api/users/{}", id);
        userService.deleteUser(id);
    }

    @PostMapping("/{id}/license/renew")
    public UserResponse renewLicense(@PathVariable("id") Long id, @Valid @RequestBody RenewLicenseRequest request) {
        log.info("POST /api/users/{}/license/renew type={}", id, request.licenseType());
        return UserResponse.from(userService.renewLicense(id, request));
    }
}
