package com.att.tdp.issueflow.projects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ProjectDtos {
    private ProjectDtos() {
    }

    public record CreateProjectRequest(@NotBlank String name, String description, @NotNull Long ownerId) {
    }

    public record UpdateProjectRequest(String name, String description) {
    }

    public record ProjectResponse(Long id, String name, String description, Long ownerId) {
        public static ProjectResponse from(Project project) {
            return new ProjectResponse(
                    project.getId(),
                    project.getName(),
                    project.getDescription(),
                    project.getOwner().getId());
        }
    }

    public record WorkloadResponse(Long userId, String username, long openTicketCount) {
    }
}
