package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.LicenseType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RenewLicenseRequest(
        @NotNull LicenseType licenseType,
        LocalDate customStartDate,
        LocalDate customExpiryDate,
        boolean revoke
) {
}
