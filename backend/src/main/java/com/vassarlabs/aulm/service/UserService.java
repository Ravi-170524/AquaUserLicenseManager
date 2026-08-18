package com.vassarlabs.aulm.service;

import com.vassarlabs.aulm.dto.CreateUserRequest;
import com.vassarlabs.aulm.dto.RenewLicenseRequest;
import com.vassarlabs.aulm.dto.UpdateUserRequest;
import com.vassarlabs.aulm.exception.ApiException;
import com.vassarlabs.aulm.model.AccessRequestStatus;
import com.vassarlabs.aulm.model.AccessRequestType;
import com.vassarlabs.aulm.model.License;
import com.vassarlabs.aulm.model.LicenseStatus;
import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.repository.AccessRequestRepository;
import com.vassarlabs.aulm.repository.UserRepository;
import com.vassarlabs.aulm.util.InputNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, AccessRequestRepository accessRequestRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Project-scoped admins only see their own project's users; superadmins see everyone. Not-yet-approved
     *  registrations are excluded here — they only show up under Pending Requests until resolved. */
    public List<User> listUsers(User admin) {
        log.info("listUsers username={} project={}", admin.getUsername(), admin.getProjectName());
        List<User> candidates = admin.isSuperAdmin()
                ? userRepository.findAll()
                : userRepository.findByProjectNameOrderByUsername(admin.getProjectName());
        Set<Long> pendingRegistrationUserIds = new HashSet<>(
                accessRequestRepository.findUserIdsByStatusAndRequestType(AccessRequestStatus.PENDING, AccessRequestType.REGISTRATION));
        return candidates.stream()
                .filter(u -> !pendingRegistrationUserIds.contains(u.getId()))
                .sorted(Comparator.comparing(User::getProjectName).thenComparing(User::getUsername))
                .toList();
    }

    public User getUser(Long id) {
        log.info("getUser id={}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User createUser(CreateUserRequest request) {
        String username = InputNormalizer.lowerTrim(request.username());
        String projectName = InputNormalizer.lowerTrim(request.projectName());
        log.info("createUser username={} project={}", username, projectName);
        if (userRepository.existsByUsernameAndProjectName(username, projectName)) {
            throw new ApiException(HttpStatus.CONFLICT, "This username already exists for this project");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(InputNormalizer.lowerTrim(request.email()));
        user.setProjectName(projectName);
        user.setAdmin(request.admin());
        user.setPermissions(request.permissions() == null || request.permissions().isEmpty()
                ? EnumSet.noneOf(com.vassarlabs.aulm.model.PermissionType.class)
                : EnumSet.copyOf(request.permissions()));

        License license = new License();
        license.setLicenseKey(UUID.randomUUID().toString());
        license.setLicenseType(request.licenseType());
        license.setStatus(LicenseStatus.ACTIVE);
        if (request.licenseType() == LicenseType.CUSTOM) {
            validateCustomDates(request.customStartDate(), request.customExpiryDate());
            license.setIssuedDate(request.customStartDate());
            license.setExpiryDate(request.customExpiryDate());
        } else {
            license.setIssuedDate(LocalDate.now());
            license.setExpiryDate(request.licenseType().computeExpiryDate(LocalDate.now()));
        }
        user.setLicense(license);

        return userRepository.save(user);
    }

    public User updateUser(Long id, UpdateUserRequest request) {
        log.info("updateUser id={}", id);
        User user = getUser(id);
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null) {
            user.setEmail(InputNormalizer.lowerTrim(request.email()));
        }
        if (request.projectName() != null) {
            String projectName = InputNormalizer.lowerTrim(request.projectName());
            if (request.projectName().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Project name cannot be blank");
            }
            if (!projectName.equals(user.getProjectName())
                    && userRepository.existsByUsernameAndProjectName(user.getUsername(), projectName)) {
                throw new ApiException(HttpStatus.CONFLICT, "This username already exists for this project");
            }
            user.setProjectName(projectName);
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
        log.info("deleteUser id={}", id);
        User user = getUser(id);
        if (user.isAdmin() && userRepository.countByAdminTrue() <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete the last remaining admin account.");
        }
        userRepository.deleteById(id);
    }

    public User renewLicense(Long id, RenewLicenseRequest request) {
        log.info("renewLicense id={} type={}", id, request.licenseType());
        User user = getUser(id);
        License license = user.getLicense();
        if (license == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User has no license to renew");
        }
        license.setLicenseType(request.licenseType());
        if (request.licenseType() == LicenseType.CUSTOM) {
            validateCustomDates(request.customStartDate(), request.customExpiryDate());
            license.setIssuedDate(request.customStartDate());
            license.setExpiryDate(request.customExpiryDate());
        } else {
            license.setExpiryDate(request.licenseType().computeExpiryDate(LocalDate.now()));
        }
        license.setStatus(request.revoke() ? LicenseStatus.REVOKED : LicenseStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void validateCustomDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Custom licenses require both a start and end date");
        }
        if (!end.isAfter(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The custom end date must be after the start date");
        }
    }
}
