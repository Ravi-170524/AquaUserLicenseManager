package com.vassarlabs.aulm.controller;

import com.vassarlabs.aulm.dto.CreateUserRequest;
import com.vassarlabs.aulm.dto.RenewLicenseRequest;
import com.vassarlabs.aulm.dto.UpdateUserRequest;
import com.vassarlabs.aulm.dto.UserResponse;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return userService.listUsers().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable("id") Long id) {
        return UserResponse.from(userService.getUser(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return UserResponse.from(user);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable("id") Long id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
    }

    @PostMapping("/{id}/license/renew")
    public UserResponse renewLicense(@PathVariable("id") Long id, @Valid @RequestBody RenewLicenseRequest request) {
        return UserResponse.from(userService.renewLicense(id, request));
    }
}
