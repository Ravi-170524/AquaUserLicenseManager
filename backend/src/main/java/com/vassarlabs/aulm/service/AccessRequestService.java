package com.vassarlabs.aulm.service;

import com.vassarlabs.aulm.dto.AdminSummary;
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
import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.PermissionType;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.repository.AccessRequestRepository;
import com.vassarlabs.aulm.repository.ProjectRepository;
import com.vassarlabs.aulm.repository.UserRepository;
import com.vassarlabs.aulm.util.InputNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    private static final Logger log = LoggerFactory.getLogger(AccessRequestService.class);
    private static final String GLOBAL_ADMIN_PROJECT = "aulm";

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final String appBaseUrl;

    public AccessRequestService(UserRepository userRepository, ProjectRepository projectRepository,
                                 AccessRequestRepository accessRequestRepository, PasswordEncoder passwordEncoder,
                                 JavaMailSender mailSender,
                                 @Value("${aulm.mail.from}") String mailFrom,
                                 @Value("${aulm.app-base-url}") String appBaseUrl) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
        this.appBaseUrl = appBaseUrl;
    }

    public User register(RegisterRequest request) {
        String username = InputNormalizer.lowerTrim(request.username());
        String projectName = InputNormalizer.lowerTrim(request.projectName());
        String email = InputNormalizer.lowerTrim(request.email());
        log.info("register username={} project={}", username, projectName);
        if (userRepository.existsByUsernameAndProjectName(username, projectName)) {
            throw new ApiException(HttpStatus.CONFLICT, "This username already exists for this project");
        }
        if (!projectRepository.existsByName(projectName)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown project — choose one from the list");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(email);
        user.setProjectName(projectName);
        user.setEnabled(true);
        user = userRepository.save(user);

        AccessRequest accessRequest = new AccessRequest();
        accessRequest.setUser(user);
        accessRequest.setRequestType(AccessRequestType.REGISTRATION);
        applyRequestedLicense(accessRequest, request.requestedLicenseType(), request.requestedStartDate(), request.requestedExpiryDate());
        accessRequest.setRequestedPermissions(request.requestedPermissions() == null || request.requestedPermissions().isEmpty()
                ? EnumSet.noneOf(PermissionType.class)
                : EnumSet.copyOf(request.requestedPermissions()));
        accessRequest.setNote(request.note());
        accessRequest.setAssignedAdmin(resolveAssignedAdmin(request.assignedAdminId(), projectName));
        accessRequestRepository.save(accessRequest);
        notifyAdmins(accessRequest);

        return user;
    }

    public AccessRequest createRequest(User requester, CreateAccessRequestRequest request) {
        log.info("createRequest username={} project={} type={}", requester.getUsername(), requester.getProjectName(), request.requestType());
        if (request.requestType() == AccessRequestType.REGISTRATION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Use the registration endpoint to create a new account");
        }
        if (accessRequestRepository.existsByUserAndStatus(requester, AccessRequestStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have a pending request");
        }

        AccessRequest accessRequest = new AccessRequest();
        accessRequest.setUser(requester);
        accessRequest.setRequestType(request.requestType());
        applyRequestedLicense(accessRequest, request.requestedLicenseType(), request.requestedStartDate(), request.requestedExpiryDate());
        accessRequest.setRequestedPermissions(request.requestedPermissions() == null || request.requestedPermissions().isEmpty()
                ? EnumSet.noneOf(PermissionType.class)
                : EnumSet.copyOf(request.requestedPermissions()));
        accessRequest.setNote(request.note());
        accessRequest.setAssignedAdmin(resolveAssignedAdmin(request.assignedAdminId(), requester.getProjectName()));
        accessRequestRepository.save(accessRequest);
        notifyAdmins(accessRequest);
        return accessRequest;
    }

    /** For a CUSTOM requested license, the requester proposes their own start/end date range. */
    private void applyRequestedLicense(AccessRequest accessRequest, LicenseType licenseType, LocalDate startDate, LocalDate expiryDate) {
        accessRequest.setRequestedLicenseType(licenseType);
        if (licenseType != LicenseType.CUSTOM) {
            return;
        }
        if (startDate == null || expiryDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Custom licenses require both a start and end date");
        }
        if (!expiryDate.isAfter(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The custom end date must be after the start date");
        }
        if (!expiryDate.isAfter(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The custom end date must be in the future");
        }
        accessRequest.setRequestedStartDate(startDate);
        accessRequest.setRequestedExpiryDate(expiryDate);
    }

    private User resolveAssignedAdmin(Long assignedAdminId, String projectName) {
        if (assignedAdminId == null) {
            return null;
        }
        String normalizedProjectName = InputNormalizer.lowerTrim(projectName);
        User admin = userRepository.findById(assignedAdminId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Selected admin was not found"));
        boolean isProjectAdmin = admin.isAdmin() && admin.getProjectName().equals(normalizedProjectName);
        if (!isProjectAdmin && !admin.isSuperAdmin()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selected admin was not found");
        }
        return admin;
    }

    /** Emails the assigned admin, or every admin in the project when "any admin" was chosen. Never fails the request on a mail error. */
    private void notifyAdmins(AccessRequest accessRequest) {
        List<User> recipients = accessRequest.getAssignedAdmin() != null
                ? List.of(accessRequest.getAssignedAdmin())
                : userRepository.findByAdminTrueAndProjectNameOrderByUsername(accessRequest.getUser().getProjectName());

        String[] emails = recipients.stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .toArray(String[]::new);
        if (emails.length == 0) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(emails);
            message.setSubject("New access request from " + accessRequest.getUser().getUsername()
                    + " (" + accessRequest.getUser().getProjectName() + ")");
            StringBuilder body = new StringBuilder();
            body.append(accessRequest.getUser().getUsername()).append(" has requested ")
                    .append(accessRequest.getRequestType().name().toLowerCase().replace('_', ' ')).append(" access.\n");
            if (accessRequest.getRequestedLicenseType() != null) {
                body.append("Requested license: ").append(accessRequest.getRequestedLicenseType()).append("\n");
            }
            if (accessRequest.getRequestedPermissions() != null && !accessRequest.getRequestedPermissions().isEmpty()) {
                body.append("Requested permissions: ").append(accessRequest.getRequestedPermissions()).append("\n");
            }
            if (accessRequest.getNote() != null && !accessRequest.getNote().isBlank()) {
                body.append("Note: ").append(accessRequest.getNote()).append("\n");
            }
            body.append("\nLog in to Aqua User & License Manager to review: ").append(appBaseUrl);
            message.setText(body.toString());
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Failed to send access request notification email for request {}: {}", accessRequest.getId(), e.getMessage());
        }
    }

    /** Admin picker for registration/access requests: anyone holding an ADMIN-tier license in this project, plus AULM (the org-wide admin project). */
    public List<AdminSummary> listAdmins(String projectName) {
        String normalizedProjectName = InputNormalizer.lowerTrim(projectName);
        log.info("listAdmins project={}", normalizedProjectName);
        List<String> eligibleProjects = normalizedProjectName.equals(GLOBAL_ADMIN_PROJECT)
                ? List.of(normalizedProjectName)
                : List.of(normalizedProjectName, GLOBAL_ADMIN_PROJECT);
        return userRepository.findByLicense_LicenseTypeAndProjectNameInOrderByUsername(LicenseType.ADMIN, eligibleProjects)
                .stream()
                .map(AdminSummary::from)
                .toList();
    }

    public List<AccessRequest> myRequests(User user) {
        return accessRequestRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /** Only requests sent to "any admin", or specifically to this admin, are visible to them. */
    public List<AccessRequest> listPending(User admin) {
        return accessRequestRepository.findVisibleTo(AccessRequestStatus.PENDING, admin);
    }

    public AccessRequest approve(Long id, ApproveAccessRequestRequest request, User admin) {
        AccessRequest accessRequest = getPendingOrThrow(id);
        assertCanResolve(accessRequest, admin);
        User user = accessRequest.getUser();

        if (accessRequest.getRequestType() != AccessRequestType.PERMISSION_CHANGE) {
            if (request.licenseType() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "License type is required to approve this request");
            }
            License license = user.getLicense();
            boolean isNewLicense = license == null;
            if (isNewLicense) {
                license = new License();
                license.setLicenseKey(UUID.randomUUID().toString());
                user.setLicense(license);
            }
            license.setLicenseType(request.licenseType());
            if (request.licenseType() == LicenseType.CUSTOM) {
                if (request.customStartDate() == null || request.customExpiryDate() == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Custom licenses require both a start and end date");
                }
                if (!request.customExpiryDate().isAfter(request.customStartDate())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "The custom end date must be after the start date");
                }
                license.setIssuedDate(request.customStartDate());
                license.setExpiryDate(request.customExpiryDate());
            } else {
                if (isNewLicense) {
                    license.setIssuedDate(LocalDate.now());
                }
                license.setExpiryDate(request.licenseType().computeExpiryDate(LocalDate.now()));
            }
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

    public AccessRequest reject(Long id, RejectAccessRequestRequest request, User admin) {
        AccessRequest accessRequest = getPendingOrThrow(id);
        assertCanResolve(accessRequest, admin);
        accessRequest.setStatus(AccessRequestStatus.REJECTED);
        accessRequest.setResolutionNote(request.reason());
        accessRequest.setResolvedAt(LocalDateTime.now());
        return accessRequestRepository.save(accessRequest);
    }

    /** A request sent to a specific admin may only be resolved by that admin, or by a superadmin. */
    private void assertCanResolve(AccessRequest accessRequest, User admin) {
        User assignedAdmin = accessRequest.getAssignedAdmin();
        if (assignedAdmin != null && !assignedAdmin.getId().equals(admin.getId()) && !admin.isSuperAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This request was sent to a different admin");
        }
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
