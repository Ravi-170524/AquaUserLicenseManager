package com.vassarlabs.aulm.service;

import com.vassarlabs.aulm.dto.CreateProjectRequest;
import com.vassarlabs.aulm.dto.ProjectResponse;
import com.vassarlabs.aulm.exception.ApiException;
import com.vassarlabs.aulm.model.Project;
import com.vassarlabs.aulm.repository.ProjectRepository;
import com.vassarlabs.aulm.util.InputNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<ProjectResponse> listProjects() {
        log.info("listProjects");
        return projectRepository.findAllByOrderByName().stream().map(ProjectResponse::from).toList();
    }

    public ProjectResponse createProject(CreateProjectRequest request) {
        String name = InputNormalizer.lowerTrim(request.name());
        log.info("createProject name={}", name);
        if (name.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Project name is required");
        }
        if (projectRepository.existsByName(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "A project with this name already exists");
        }
        Project project = new Project();
        project.setName(name);
        return ProjectResponse.from(projectRepository.save(project));
    }
}
