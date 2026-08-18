package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.PermissionType;

import java.time.LocalDate;
import java.util.Set;

public record ApproveAccessRequestRequest(
        LicenseType licenseType,
        LocalDate customStartDate,
        LocalDate customExpiryDate,
        Set<PermissionType> permissions
) {
}
