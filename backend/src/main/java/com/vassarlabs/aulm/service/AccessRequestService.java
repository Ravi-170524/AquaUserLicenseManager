package com.vassarlabs.aulm.service;

import com.vassarlabs.aulm.dto.ApproveAccessRequestRequest;
import com.vassarlabs.aulm.dto.CreateAccessRequestRequest;
import com.vassarlabs.aulm.dto.RegisterRequest;
import com.vassarlabs.aulm.dto.RejectAccessRequestRequest;
import com.vassarlabs.aulm.exception.ApiException;
import com.vassarlabs.aulm.model.AccessRequest;
import com.vassarlabs.aulm.model.AccessRequestStatus;
import com.vassarlabs.aulm.model.AccessRequestType;
import com.vassarlabs.aulm.model.License;
import com.vassarlabs.aulm.model.LicenseStatus;
import com.vassarlabs.aulm.model.PermissionType;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.repository.AccessRequestRepository;
import com.vassarlabs.aulm.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AccessRequestService {

    private final UserRepository userRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public AccessRequestService(UserRepository userRepository, AccessRequestRepository accessRequestRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsernameAndProjectName(request.username(), request.projectName())) {
            throw new ApiException(HttpStatus.CONFLICT, "This username already exists for this project");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setProjectName(request.projectName());
        user.setEnabled(true);
        user = userRepository.save(user);

        AccessRequest accessRequest = new AccessRequest();
        accessRequest.setUser(user);
        accessRequest.setRequestType(AccessRequestType.REGISTRATION);
        accessRequest.setRequestedLicenseType(request.requestedLicenseType());
        accessRequest.setNote(request.note());
        accessRequestRepository.save(accessRequest);

        return user;
    }

    public AccessRequest createRequest(User requester, CreateAccessRequestRequest request) {
        if (request.requestType() == AccessRequestType.REGISTRATION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Use the registration endpoint to create a new account");
        }
        if (accessRequestRepository.existsByUserAndStatus(requester, AccessRequestStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have a pending request");
        }

        AccessRequest accessRequest = new AccessRequest();
        accessRequest.setUser(requester);
        accessRequest.setRequestType(request.requestType());
        accessRequest.setRequestedLicenseType(request.requestedLicenseType());
        accessRequest.setRequestedPermissions(request.requestedPermissions() == null || request.requestedPermissions().isEmpty()
                ? EnumSet.noneOf(PermissionType.class)
                : EnumSet.copyOf(request.requestedPermissions()));
        accessRequest.setNote(request.note());
        return accessRequestRepository.save(accessRequest);
    }

    public List<AccessRequest> myRequests(User user) {
        return accessRequestRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<AccessRequest> listPending() {
        return accessRequestRepository.findByStatusOrderByCreatedAtAsc(AccessRequestStatus.PENDING);
    }

    public AccessRequest approve(Long id, ApproveAccessRequestRequest request) {
        AccessRequest accessRequest = getPendingOrThrow(id);
        User user = accessRequest.getUser();

        if (accessRequest.getRequestType() != AccessRequestType.PERMISSION_CHANGE) {
            if (request.licenseType() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "License type is required to approve this request");
            }
            License license = user.getLicense();
            if (license == null) {
                license = new License();
                license.setLicenseKey(UUID.randomUUID().toString());
                license.setIssuedDate(LocalDate.now());
                user.setLicense(license);
            }
            license.setLicenseType(request.licenseType());
            license.setExpiryDate(request.licenseType().computeExpiryDate(LocalDate.now()));
            license.setStatus(LicenseStatus.ACTIVE);
        }

        if (request.permissions() != null) {
            user.setPermissions(request.permissions().isEmpty()
                    ? EnumSet.noneOf(PermissionType.class)
                    : EnumSet.copyOf(request.permissions()));
        }
        userRepository.save(user);

        accessRequest.setStatus(AccessRequestStatus.APPROVED);
        accessRequest.setResolvedAt(LocalDateTime.now());
        return accessRequestRepository.save(accessRequest);
    }

    public AccessRequest reject(Long id, RejectAccessRequestRequest request) {
        AccessRequest accessRequest = getPendingOrThrow(id);
        accessRequest.setStatus(AccessRequestStatus.REJECTED);
        accessRequest.setResolutionNote(request.reason());
        accessRequest.setResolvedAt(LocalDateTime.now());
        return accessRequestRepository.save(accessRequest);
    }

    private AccessRequest getPendingOrThrow(Long id) {
        AccessRequest accessRequest = accessRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        if (accessRequest.getStatus() != AccessRequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "This request has already been resolved");
        }
        return accessRequest;
    }
}
