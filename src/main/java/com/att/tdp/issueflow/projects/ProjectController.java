package com.att.tdp.issueflow.projects;

import com.att.tdp.issueflow.auth.CurrentUser;
import com.att.tdp.issueflow.projects.ProjectDtos.CreateProjectRequest;
import com.att.tdp.issueflow.projects.ProjectDtos.ProjectResponse;
import com.att.tdp.issueflow.projects.ProjectDtos.UpdateProjectRequest;
import com.att.tdp.issueflow.projects.ProjectDtos.WorkloadResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<ProjectResponse> findAll() {
        return projectService.findAll();
    }

    @GetMapping("/{projectId}")
    public ProjectResponse findById(@PathVariable Long projectId) {
        return projectService.findById(projectId);
    }

    @PostMapping
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(request, currentUser.get());
    }

    @PatchMapping("/{projectId}")
    public void update(@PathVariable Long projectId, @RequestBody UpdateProjectRequest request) {
        projectService.update(projectId, request, currentUser.get());
    }

    @DeleteMapping("/{projectId}")
    public void delete(@PathVariable Long projectId) {
        projectService.delete(projectId, currentUser.get());
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProjectResponse> deleted() {
        return projectService.deleted();
    }

    @PostMapping("/{projectId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public void restore(@PathVariable Long projectId) {
        projectService.restore(projectId, currentUser.get());
    }

    @GetMapping("/{projectId}/workload")
    public List<WorkloadResponse> workload(@PathVariable Long projectId) {
        return projectService.workload(projectId);
    }
}
