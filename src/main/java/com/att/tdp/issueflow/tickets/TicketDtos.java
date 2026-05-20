package com.att.tdp.issueflow.tickets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class TicketDtos {
    private TicketDtos() {
    }

    public record CreateTicketRequest(
            @NotBlank String title,
            String description,
            @NotNull TicketStatus status,
            @NotNull TicketPriority priority,
            @NotNull TicketType type,
            @NotNull Long projectId,
            Long assigneeId,
            Instant dueDate
    ) {
    }

    public record UpdateTicketRequest(
            String title,
            String description,
            TicketStatus status,
            TicketPriority priority,
            Long assigneeId,
            Instant dueDate
    ) {
    }

    public record TicketResponse(
            Long id,
            String title,
            String description,
            TicketStatus status,
            TicketPriority priority,
            TicketType type,
            Long projectId,
            Long assigneeId,
            Instant dueDate,
            boolean isOverdue
    ) {
        public static TicketResponse from(Ticket ticket) {
            return new TicketResponse(
                    ticket.getId(),
                    ticket.getTitle(),
                    ticket.getDescription(),
                    ticket.getStatus(),
                    ticket.getPriority(),
                    ticket.getType(),
                    ticket.getProject().getId(),
                    ticket.getAssignee() == null ? null : ticket.getAssignee().getId(),
                    ticket.getDueDate(),
                    ticket.isOverdue());
        }
    }

    public record DependencyRequest(@NotNull Long blockedBy) {
    }

    public record DependencyResponse(Long id, String title, TicketStatus status) {
        public static DependencyResponse from(Ticket ticket) {
            return new DependencyResponse(ticket.getId(), ticket.getTitle(), ticket.getStatus());
        }
    }

    public record CsvImportResponse(int created, int failed, List<String> errors) {
    }
}
