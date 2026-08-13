package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.PermissionType;

import java.util.Set;

public record ApproveAccessRequestRequest(
        LicenseType licenseType,
        Set<PermissionType> permissions
) {
}
