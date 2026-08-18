package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.User;

public record AdminSummary(Long id, String username, String fullName, String projectName, boolean superAdmin) {
    public static AdminSummary from(User user) {
        return new AdminSummary(user.getId(), user.getUsername(), user.getFullName(), user.getProjectName(), user.isSuperAdmin());
    }
}
