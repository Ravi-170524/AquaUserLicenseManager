package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.PermissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String password,
        String fullName,
        String email,
        boolean admin,
        Set<PermissionType> permissions,
        @NotNull LicenseType licenseType,
        LocalDate expiryDate
) {
}
