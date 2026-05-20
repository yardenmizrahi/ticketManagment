package com.att.tdp.issueflow.projects;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.common.Require;
import com.att.tdp.issueflow.projects.ProjectDtos.CreateProjectRequest;
import com.att.tdp.issueflow.projects.ProjectDtos.ProjectResponse;
import com.att.tdp.issueflow.projects.ProjectDtos.UpdateProjectRequest;
import com.att.tdp.issueflow.projects.ProjectDtos.WorkloadResponse;
import com.att.tdp.issueflow.tickets.TicketRepository;
import com.att.tdp.issueflow.tickets.TicketStatus;
import com.att.tdp.issueflow.users.Role;
import com.att.tdp.issueflow.users.User;
import com.att.tdp.issueflow.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll() {
        return projectRepository.findByDeletedAtIsNullOrderByIdAsc().stream().map(ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        return ProjectResponse.from(loadActive(id));
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, User actor) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setOwner(Require.found(userRepository.findById(request.ownerId()), "Owner user not found"));
        Project saved = projectRepository.save(project);
        auditService.user("CREATE", "PROJECT", saved.getId(), actor);
        return ProjectResponse.from(saved);
    }

    @Transactional
    public void update(Long id, UpdateProjectRequest request, User actor) {
        Project project = loadActive(id);
        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        auditService.user("UPDATE", "PROJECT", id, actor);
    }

    @Transactional
    public void delete(Long id, User actor) {
        Project project = loadActive(id);
        project.setDeletedAt(Instant.now());
        auditService.user("DELETE", "PROJECT", id, actor);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> deleted() {
        return projectRepository.findByDeletedAtIsNotNullOrderByIdAsc().stream().map(ProjectResponse::from).toList();
    }

    @Transactional
    public void restore(Long id, User actor) {
        Project project = Require.found(projectRepository.findById(id), "Project not found");
        project.setDeletedAt(null);
        auditService.user("RESTORE", "PROJECT", id, actor);
    }

    @Transactional(readOnly = true)
    public List<WorkloadResponse> workload(Long projectId) {
        loadActive(projectId);
        return userRepository.findByRoleOrderByCreatedAtAscIdAsc(Role.DEVELOPER).stream()
                .map(user -> new WorkloadResponse(
                        user.getId(),
                        user.getUsername(),
                        ticketRepository.countByProjectIdAndAssigneeIdAndStatusNotAndDeletedAtIsNull(
                                projectId, user.getId(), TicketStatus.DONE)))
                .sorted(Comparator.comparingLong(WorkloadResponse::openTicketCount)
                        .thenComparing(WorkloadResponse::userId))
                .toList();
    }

    public Project loadActive(Long id) {
        return Require.found(projectRepository.findByIdAndDeletedAtIsNull(id), "Project not found");
    }
}
