package com.att.tdp.issueflow.tickets;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.common.ApiException;
import com.att.tdp.issueflow.projects.Project;
import com.att.tdp.issueflow.projects.ProjectService;
import com.att.tdp.issueflow.tickets.TicketDtos.CreateTicketRequest;
import com.att.tdp.issueflow.tickets.TicketDtos.UpdateTicketRequest;
import com.att.tdp.issueflow.users.Role;
import com.att.tdp.issueflow.users.User;
import com.att.tdp.issueflow.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock
    TicketRepository ticketRepository;
    @Mock
    TicketDependencyRepository dependencyRepository;
    @Mock
    ProjectService projectService;
    @Mock
    UserRepository userRepository;
    @Mock
    AuditService auditService;

    @InjectMocks
    TicketService ticketService;

    @Test
    void rejectsBackwardStatusTransition() {
        Ticket ticket = ticket(1L, TicketStatus.IN_REVIEW, TicketPriority.HIGH);
        when(ticketRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ticket));

        UpdateTicketRequest request = new UpdateTicketRequest(null, null, TicketStatus.TODO, null, null, null);

        assertThatThrownBy(() -> ticketService.update(1L, request, actor()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot move backward");
    }

    @Test
    void rejectsUpdateAfterDone() {
        Ticket ticket = ticket(1L, TicketStatus.DONE, TicketPriority.HIGH);
        when(ticketRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ticket));

        UpdateTicketRequest request = new UpdateTicketRequest("new", null, null, null, null, null);

        assertThatThrownBy(() -> ticketService.update(1L, request, actor()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("DONE tickets");
    }

    @Test
    void rejectsDoneWhenDependenciesAreUnresolved() {
        Ticket ticket = ticket(1L, TicketStatus.IN_REVIEW, TicketPriority.HIGH);
        when(ticketRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ticket));
        when(dependencyRepository.existsByBlockedTicketIdAndBlockerTicketStatusNotAndBlockerTicketDeletedAtIsNull(1L, TicketStatus.DONE))
                .thenReturn(true);

        UpdateTicketRequest request = new UpdateTicketRequest(null, null, TicketStatus.DONE, null, null, null);

        assertThatThrownBy(() -> ticketService.update(1L, request, actor()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("unresolved dependencies");
    }

    @Test
    void autoAssignsLeastLoadedDeveloperOnCreate() {
        Project project = project(10L);
        User busier = developer(2L, "busy", Instant.parse("2026-01-01T00:00:00Z"));
        User quieter = developer(3L, "quiet", Instant.parse("2026-01-02T00:00:00Z"));
        when(projectService.loadActive(10L)).thenReturn(project);
        when(userRepository.findByRoleOrderByCreatedAtAscIdAsc(Role.DEVELOPER)).thenReturn(List.of(busier, quieter));
        when(ticketRepository.countByProjectIdAndAssigneeIdAndStatusNotAndDeletedAtIsNull(10L, 2L, TicketStatus.DONE)).thenReturn(4L);
        when(ticketRepository.countByProjectIdAndAssigneeIdAndStatusNotAndDeletedAtIsNull(10L, 3L, TicketStatus.DONE)).thenReturn(1L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        CreateTicketRequest request = new CreateTicketRequest(
                "Fix login", "broken", TicketStatus.TODO, TicketPriority.HIGH, TicketType.BUG, 10L, null, null);

        var response = ticketService.create(request, actor());

        assertThat(response.assigneeId()).isEqualTo(3L);
        verify(auditService).system("AUTO_ASSIGN", "TICKET", 99L);
    }

    @Test
    void escalatesOverdueTicketsIdempotently() {
        Ticket low = ticket(1L, TicketStatus.TODO, TicketPriority.LOW);
        Ticket critical = ticket(2L, TicketStatus.IN_PROGRESS, TicketPriority.CRITICAL);
        when(ticketRepository.findOverdueUnresolved(any())).thenReturn(List.of(low, critical));

        ticketService.escalateOverdueTickets();

        assertThat(low.getPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(critical.isOverdue()).isTrue();
        verify(auditService, times(2)).system(eq("AUTO_ESCALATE"), eq("TICKET"), anyLong());
    }

    private Ticket ticket(Long id, TicketStatus status, TicketPriority priority) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle("ticket");
        ticket.setDescription("description");
        ticket.setProject(project(10L));
        ticket.setStatus(status);
        ticket.setPriority(priority);
        ticket.setType(TicketType.BUG);
        return ticket;
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setName("project");
        project.setOwner(actor());
        return project;
    }

    private User actor() {
        User user = developer(1L, "actor", Instant.parse("2026-01-01T00:00:00Z"));
        user.setRole(Role.ADMIN);
        return user;
    }

    private User developer(Long id, String username, Instant createdAt) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFullName(username);
        user.setRole(Role.DEVELOPER);
        user.setCreatedAt(createdAt);
        user.setPasswordHash("hash");
        return user;
    }
}
