package com.vassarlabs.aulm.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "access_requests")
public class AccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The admin the requester chose to send this to. Informational only — every admin can still see and resolve any request. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_admin_id")
    private User assignedAdmin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessRequestStatus status = AccessRequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private LicenseType requestedLicenseType;

    /** Only set when requestedLicenseType is CUSTOM. */
    @Column(name = "requested_start_date")
    private LocalDate requestedStartDate;

    @Column(name = "requested_expiry_date")
    private LocalDate requestedExpiryDate;

    @ElementCollection(targetClass = PermissionType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "access_request_permissions", joinColumns = @JoinColumn(name = "access_request_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<PermissionType> requestedPermissions = EnumSet.noneOf(PermissionType.class);

    @Column(length = 1000)
    private String note;

    @Column(length = 1000)
    private String resolutionNote;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getAssignedAdmin() {
        return assignedAdmin;
    }

    public void setAssignedAdmin(User assignedAdmin) {
        this.assignedAdmin = assignedAdmin;
    }

    public AccessRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(AccessRequestType requestType) {
        this.requestType = requestType;
    }

    public AccessRequestStatus getStatus() {
        return status;
    }

    public void setStatus(AccessRequestStatus status) {
        this.status = status;
    }

    public LicenseType getRequestedLicenseType() {
        return requestedLicenseType;
    }

    public void setRequestedLicenseType(LicenseType requestedLicenseType) {
        this.requestedLicenseType = requestedLicenseType;
    }

    public LocalDate getRequestedStartDate() {
        return requestedStartDate;
    }

    public void setRequestedStartDate(LocalDate requestedStartDate) {
        this.requestedStartDate = requestedStartDate;
    }

    public LocalDate getRequestedExpiryDate() {
        return requestedExpiryDate;
    }

    public void setRequestedExpiryDate(LocalDate requestedExpiryDate) {
        this.requestedExpiryDate = requestedExpiryDate;
    }

    public Set<PermissionType> getRequestedPermissions() {
        return requestedPermissions;
    }

    public void setRequestedPermissions(Set<PermissionType> requestedPermissions) {
        this.requestedPermissions = requestedPermissions;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
