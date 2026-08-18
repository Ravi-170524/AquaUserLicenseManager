package com.vassarlabs.aulm.dto;

import com.vassarlabs.aulm.model.Project;

import java.util.UUID;

public record ProjectResponse(Long id, UUID uuid, String name) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getUuid(), project.getName());
    }
}
