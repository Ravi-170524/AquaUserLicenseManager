package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.AccessRequestType;
import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.PermissionType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

public record CreateAccessRequestRequest(
        @NotNull AccessRequestType requestType,
        LicenseType requestedLicenseType,
        LocalDate requestedStartDate,
        LocalDate requestedExpiryDate,
        Set<PermissionType> requestedPermissions,
        String note,
        @NotNull Long assignedAdminId
) {
}
