package com.vassarlabs.aulm.controller;

import com.vassarlabs.aulm.dto.CreateProjectRequest;
import com.vassarlabs.aulm.dto.ProjectResponse;
import com.vassarlabs.aulm.service.ProjectService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /** Lists all projects — used to populate the project pickers on login/register/forgot-password. */
    @GetMapping("/getProjects")
    public List<ProjectResponse> getProjects() {
        log.info("GET /api/projects/getProjects");
        return projectService.getProjects();
    }

    /** Creates a new project (superadmin only). */
    @PostMapping("/createProject")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        log.info("POST /api/projects/createProject name={}", request.name());
        return projectService.createProject(request);
    }
}
