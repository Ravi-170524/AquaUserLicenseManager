package com.vassarlabs.aulm.service;

import com.vassarlabs.aulm.dto.CreateUserRequest;
import com.vassarlabs.aulm.dto.RenewLicenseRequest;
import com.vassarlabs.aulm.dto.UpdateUserRequest;
import com.vassarlabs.aulm.exception.ApiException;
import com.vassarlabs.aulm.model.License;
import com.vassarlabs.aulm.model.LicenseStatus;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByUsernameAndProjectName(request.username(), request.projectName())) {
            throw new ApiException(HttpStatus.CONFLICT, "This username already exists for this project");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setProjectName(request.projectName());
        user.setAdmin(request.admin());
        user.setPermissions(request.permissions() == null || request.permissions().isEmpty()
                ? EnumSet.noneOf(com.vassarlabs.aulm.model.PermissionType.class)
                : EnumSet.copyOf(request.permissions()));

        License license = new License();
        license.setLicenseKey(UUID.randomUUID().toString());
        license.setLicenseType(request.licenseType());
        license.setStatus(LicenseStatus.ACTIVE);
        license.setIssuedDate(LocalDate.now());
        license.setExpiryDate(request.licenseType().computeExpiryDate(LocalDate.now()));
        user.setLicense(license);

        return userRepository.save(user);
    }

    public User updateUser(Long id, UpdateUserRequest request) {
        User user = getUser(id);
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.projectName() != null) {
            if (request.projectName().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Project name cannot be blank");
            }
            if (!request.projectName().equals(user.getProjectName())
                    && userRepository.existsByUsernameAndProjectName(user.getUsername(), request.projectName())) {
                throw new ApiException(HttpStatus.CONFLICT, "This username already exists for this project");
            }
            user.setProjectName(request.projectName());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.admin() != null) {
            user.setAdmin(request.admin());
        }
        if (request.permissions() != null) {
            user.setPermissions(request.permissions().isEmpty()
                    ? EnumSet.noneOf(com.vassarlabs.aulm.model.PermissionType.class)
                    : EnumSet.copyOf(request.permissions()));
        }
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = getUser(id);
        if (user.isAdmin() && userRepository.countByAdminTrue() <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete the last remaining admin account.");
        }
        userRepository.deleteById(id);
    }

    public User renewLicense(Long id, RenewLicenseRequest request) {
        User user = getUser(id);
        License license = user.getLicense();
        if (license == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User has no license to renew");
        }
        license.setLicenseType(request.licenseType());
        license.setExpiryDate(request.licenseType().computeExpiryDate(LocalDate.now()));
        license.setStatus(request.revoke() ? LicenseStatus.REVOKED : LicenseStatus.ACTIVE);
        return userRepository.save(user);
    }
}
