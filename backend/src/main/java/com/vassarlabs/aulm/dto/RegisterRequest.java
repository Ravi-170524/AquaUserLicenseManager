package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.LicenseType;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password,
        String fullName,
        String email,
        @NotBlank String projectName,
        LicenseType requestedLicenseType,
        String note
) {
}
