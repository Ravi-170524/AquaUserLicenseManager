package com.vassarlabs.aulm.ui.model;

import java.util.Set;

public record UpdateUserPayload(
        String fullName,
        String email,
        String projectName,
        Boolean enabled,
        Boolean admin,
        Set<String> permissions,
        String newPassword
) {
}
