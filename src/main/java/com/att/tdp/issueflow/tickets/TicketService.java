package com.att.tdp.issueflow.tickets;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.common.ApiException;
import com.att.tdp.issueflow.common.Require;
import com.att.tdp.issueflow.projects.Project;
import com.att.tdp.issueflow.projects.ProjectService;
import com.att.tdp.issueflow.tickets.TicketDtos.*;
import com.att.tdp.issueflow.users.Role;
import com.att.tdp.issueflow.users.User;
import com.att.tdp.issueflow.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TicketDependencyRepository dependencyRepository;
    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<TicketResponse> findByProject(Long projectId) {
        projectService.loadActive(projectId);
        return ticketRepository.findByProjectIdAndDeletedAtIsNullOrderByIdAsc(projectId)
                .stream().map(TicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse findById(Long id) {
        return TicketResponse.from(loadActive(id));
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request, User actor) {
        Project project = projectService.loadActive(request.projectId());
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setStatus(request.status());
        ticket.setPriority(request.priority());
        ticket.setType(request.type());
        ticket.setProject(project);
        ticket.setDueDate(request.dueDate());
        ticket.setAssignee(request.assigneeId() == null ? chooseAssignee(project.getId()) : loadDeveloper(request.assigneeId()));
        Ticket saved = ticketRepository.save(ticket);
        auditService.user("CREATE", "TICKET", saved.getId(), actor);
        if (request.assigneeId() == null && saved.getAssignee() != null) {
            auditService.system("AUTO_ASSIGN", "TICKET", saved.getId());
        }
        return TicketResponse.from(saved);
    }

    @Transactional
    public void update(Long id, UpdateTicketRequest request, User actor) {
        Ticket ticket = loadActive(id);
        Require.conflict(ticket.getStatus() != TicketStatus.DONE, "DONE tickets cannot be updated");

        if (request.status() != null) {
            validateForwardStatus(ticket.getStatus(), request.status());
            if (request.status() == TicketStatus.DONE) {
                Require.valid(!dependencyRepository.existsByBlockedTicketIdAndBlockerTicketStatusNotAndBlockerTicketDeletedAtIsNull(
                        id, TicketStatus.DONE), "Ticket has unresolved dependencies");
            }
            ticket.setStatus(request.status());
        }
        if (request.title() != null) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
            ticket.setOverdue(false);
        }
        if (request.assigneeId() != null) {
            ticket.setAssignee(loadDeveloper(request.assigneeId()));
        }
        if (request.dueDate() != null) {
            ticket.setDueDate(request.dueDate());
        }
        auditService.user("UPDATE", "TICKET", id, actor);
    }

    @Transactional
    public void delete(Long id, User actor) {
        Ticket ticket = loadActive(id);
        ticket.setDeletedAt(Instant.now());
        auditService.user("DELETE", "TICKET", id, actor);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> deleted(Long projectId) {
        return ticketRepository.findByProjectIdAndDeletedAtIsNotNullOrderByIdAsc(projectId)
                .stream().map(TicketResponse::from).toList();
    }

    @Transactional
    public void restore(Long id, User actor) {
        Ticket ticket = Require.found(ticketRepository.findById(id), "Ticket not found");
        ticket.setDeletedAt(null);
        auditService.user("RESTORE", "TICKET", id, actor);
    }

    @Transactional
    public void addDependency(Long ticketId, Long blockerId, User actor) {
        Require.valid(!ticketId.equals(blockerId), "A ticket cannot depend on itself");
        Ticket ticket = loadActive(ticketId);
        Ticket blocker = loadActive(blockerId);
        Require.valid(ticket.getProject().getId().equals(blocker.getProject().getId()),
                "Dependencies must be inside the same project");
        if (dependencyRepository.findByBlockedTicketIdAndBlockerTicketId(ticketId, blockerId).isPresent()) {
            return;
        }
        TicketDependency dependency = new TicketDependency();
        dependency.setBlockedTicket(ticket);
        dependency.setBlockerTicket(blocker);
        dependencyRepository.save(dependency);
        auditService.user("ADD_DEPENDENCY", "TICKET", ticketId, actor);
    }

    @Transactional(readOnly = true)
    public List<DependencyResponse> listDependencies(Long ticketId) {
        loadActive(ticketId);
        return dependencyRepository.findByBlockedTicketId(ticketId).stream()
                .map(TicketDependency::getBlockerTicket)
                .filter(ticket -> ticket.getDeletedAt() == null)
                .map(DependencyResponse::from)
                .toList();
    }

    @Transactional
    public void removeDependency(Long ticketId, Long blockerId, User actor) {
        TicketDependency dependency = Require.found(
                dependencyRepository.findByBlockedTicketIdAndBlockerTicketId(ticketId, blockerId),
                "Dependency not found");
        dependencyRepository.delete(dependency);
        auditService.user("REMOVE_DEPENDENCY", "TICKET", ticketId, actor);
    }

    @Transactional(readOnly = true)
    public String exportCsv(Long projectId) {
        List<Ticket> tickets = ticketRepository.findByProjectIdAndDeletedAtIsNullOrderByIdAsc(projectId);
        try {
            StringWriter writer = new StringWriter();
            CSVPrinter printer = CSVFormat.DEFAULT.builder()
                    .setHeader("id", "title", "description", "status", "priority", "type", "assigneeId")
                    .build()
                    .print(writer);
            for (Ticket ticket : tickets) {
                printer.printRecord(ticket.getId(), ticket.getTitle(), ticket.getDescription(), ticket.getStatus(),
                        ticket.getPriority(), ticket.getType(),
                        ticket.getAssignee() == null ? null : ticket.getAssignee().getId());
            }
            printer.flush();
            return writer.toString();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not export tickets");
        }
    }

    @Transactional
    public CsvImportResponse importCsv(MultipartFile file, Long projectId, User actor) {
        Project project = projectService.loadActive(projectId);
        int created = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            int row = 1;
            for (CSVRecord record : parser) {
                row++;
                try {
                    Ticket ticket = new Ticket();
                    ticket.setTitle(required(record, "title"));
                    ticket.setDescription(record.get("description"));
                    ticket.setStatus(TicketStatus.valueOf(required(record, "status")));
                    ticket.setPriority(TicketPriority.valueOf(required(record, "priority")));
                    ticket.setType(TicketType.valueOf(required(record, "type")));
                    ticket.setProject(project);
                    String assigneeId = record.get("assigneeId");
                    boolean autoAssign = assigneeId == null || assigneeId.isBlank();
                    ticket.setAssignee(autoAssign ? chooseAssignee(projectId) : loadDeveloper(Long.valueOf(assigneeId)));
                    Ticket saved = ticketRepository.save(ticket);
                    auditService.user("CREATE", "TICKET", saved.getId(), actor);
                    if (autoAssign && saved.getAssignee() != null) {
                        auditService.system("AUTO_ASSIGN", "TICKET", saved.getId());
                    }
                    created++;
                } catch (Exception ex) {
                    errors.add("row " + row + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read CSV file");
        }
        return new CsvImportResponse(created, errors.size(), errors);
    }

    @Scheduled(fixedDelayString = "60000")
    @Transactional
    public void escalateOverdueTickets() {
        for (Ticket ticket : ticketRepository.findOverdueUnresolved(Instant.now())) {
            if (ticket.getPriority() == TicketPriority.CRITICAL) {
                if (!ticket.isOverdue()) {
                    ticket.setOverdue(true);
                    auditService.system("AUTO_ESCALATE", "TICKET", ticket.getId());
                }
            } else {
                ticket.setPriority(nextPriority(ticket.getPriority()));
                auditService.system("AUTO_ESCALATE", "TICKET", ticket.getId());
            }
        }
    }

    public Ticket loadActive(Long id) {
        return Require.found(ticketRepository.findByIdAndDeletedAtIsNull(id), "Ticket not found");
    }

    private User chooseAssignee(Long projectId) {
        return userRepository.findByRoleOrderByCreatedAtAscIdAsc(Role.DEVELOPER).stream()
                .min(Comparator.comparingLong((User user) -> ticketRepository.countByProjectIdAndAssigneeIdAndStatusNotAndDeletedAtIsNull(
                        projectId, user.getId(), TicketStatus.DONE)).thenComparing(User::getCreatedAt).thenComparing(User::getId))
                .orElse(null);
    }

    private User loadDeveloper(Long userId) {
        User user = Require.found(userRepository.findById(userId), "Assignee user not found");
        Require.valid(user.getRole() == Role.DEVELOPER, "Assignee must be a DEVELOPER");
        return user;
    }

    private void validateForwardStatus(TicketStatus current, TicketStatus next) {
        Require.valid(next.ordinal() >= current.ordinal(), "Ticket status cannot move backward");
    }

    private TicketPriority nextPriority(TicketPriority priority) {
        return switch (priority) {
            case LOW -> TicketPriority.MEDIUM;
            case MEDIUM -> TicketPriority.HIGH;
            case HIGH -> TicketPriority.CRITICAL;
            case CRITICAL -> TicketPriority.CRITICAL;
        };
    }

    private String required(CSVRecord record, String name) {
        String value = record.get(name);
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, name + " is required");
        }
        return value;
    }
}
