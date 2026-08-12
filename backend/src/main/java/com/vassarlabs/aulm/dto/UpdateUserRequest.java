package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.PermissionType;

import java.util.Set;

public record UpdateUserRequest(
        String fullName,
        String email,
        Boolean enabled,
        Boolean admin,
        Set<PermissionType> permissions,
        String newPassword
) {
}
