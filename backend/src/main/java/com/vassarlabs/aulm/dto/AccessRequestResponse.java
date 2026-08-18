package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.AccessRequest;
import com.vassarlabs.aulm.model.AccessRequestStatus;
import com.vassarlabs.aulm.model.AccessRequestType;
import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.PermissionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public class AccessRequestResponse {
    private Long id;
    private Long userId;
    private String username;
    private String projectName;
    private String assignedAdminUsername;
    private AccessRequestType requestType;
    private AccessRequestStatus status;
    private LicenseType requestedLicenseType;
    private LocalDate requestedStartDate;
    private LocalDate requestedExpiryDate;
    private Set<PermissionType> requestedPermissions;
    private Set<PermissionType> currentPermissions;
    private String note;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public static AccessRequestResponse from(AccessRequest request) {
        AccessRequestResponse dto = new AccessRequestResponse();
        dto.id = request.getId();
        dto.userId = request.getUser().getId();
        dto.username = request.getUser().getUsername();
        dto.projectName = request.getUser().getProjectName();
        dto.assignedAdminUsername = request.getAssignedAdmin() != null ? request.getAssignedAdmin().getUsername() : null;
        dto.requestType = request.getRequestType();
        dto.status = request.getStatus();
        dto.requestedLicenseType = request.getRequestedLicenseType();
        dto.requestedStartDate = request.getRequestedStartDate();
        dto.requestedExpiryDate = request.getRequestedExpiryDate();
        dto.requestedPermissions = request.getRequestedPermissions();
        dto.currentPermissions = request.getUser().getPermissions();
        dto.note = request.getNote();
        dto.resolutionNote = request.getResolutionNote();
        dto.createdAt = request.getCreatedAt();
        dto.resolvedAt = request.getResolvedAt();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getAssignedAdminUsername() {
        return assignedAdminUsername;
    }

    public AccessRequestType getRequestType() {
        return requestType;
    }

    public AccessRequestStatus getStatus() {
        return status;
    }

    public LicenseType getRequestedLicenseType() {
        return requestedLicenseType;
    }

    public LocalDate getRequestedStartDate() {
        return requestedStartDate;
    }

    public LocalDate getRequestedExpiryDate() {
        return requestedExpiryDate;
    }

    public Set<PermissionType> getRequestedPermissions() {
        return requestedPermissions;
    }

    public Set<PermissionType> getCurrentPermissions() {
        return currentPermissions;
    }

    public String getNote() {
        return note;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
}
