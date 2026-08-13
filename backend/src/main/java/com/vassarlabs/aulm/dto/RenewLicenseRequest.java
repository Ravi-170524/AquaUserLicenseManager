package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.LicenseType;
import jakarta.validation.constraints.NotNull;

public record RenewLicenseRequest(
        @NotNull LicenseType licenseType,
        boolean revoke
) {
}
