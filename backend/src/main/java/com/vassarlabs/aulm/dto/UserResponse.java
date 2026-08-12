package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.LicenseStatus;
import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.PermissionType;
import com.vassarlabs.aulm.model.User;

import java.time.LocalDate;
import java.util.Set;

public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private boolean enabled;
    private boolean admin;
    private Set<PermissionType> permissions;
    private String licenseKey;
    private LicenseType licenseType;
    private LicenseStatus licenseStatus;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private boolean licenseValid;

    public static UserResponse from(User user) {
        UserResponse dto = new UserResponse();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.fullName = user.getFullName();
        dto.email = user.getEmail();
        dto.enabled = user.isEnabled();
        dto.admin = user.isAdmin();
        dto.permissions = user.getPermissions();
        if (user.getLicense() != null) {
            dto.licenseKey = user.getLicense().getLicenseKey();
            dto.licenseType = user.getLicense().getLicenseType();
            dto.licenseStatus = user.getLicense().getStatus();
            dto.issuedDate = user.getLicense().getIssuedDate();
            dto.expiryDate = user.getLicense().getExpiryDate();
            dto.licenseValid = user.getLicense().isValid();
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAdmin() {
        return admin;
    }

    public Set<PermissionType> getPermissions() {
        return permissions;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public LicenseType getLicenseType() {
        return licenseType;
    }

    public LicenseStatus getLicenseStatus() {
        return licenseStatus;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public boolean isLicenseValid() {
        return licenseValid;
    }
}
