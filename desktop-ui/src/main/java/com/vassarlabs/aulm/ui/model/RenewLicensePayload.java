package com.vassarlabs.aulm.ui.model;

import java.time.LocalDate;

public record RenewLicensePayload(
        String licenseType,
        LocalDate expiryDate,
        boolean revoke
) {
}
