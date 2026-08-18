package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.PermissionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password,
        String fullName,
        @NotBlank @Email String email,
        @NotBlank String projectName,
        LicenseType requestedLicenseType,
        LocalDate requestedStartDate,
        LocalDate requestedExpiryDate,
        Set<PermissionType> requestedPermissions,
        String note,
        @NotNull Long assignedAdminId
) {
}
