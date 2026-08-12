package com.vassarlabs.aulm.ui.model;

import java.time.LocalDate;
import java.util.Set;

public record CreateUserPayload(
        String username,
        String password,
        String fullName,
        String email,
        boolean admin,
        Set<String> permissions,
        String licenseType,
        LocalDate expiryDate
) {
}
