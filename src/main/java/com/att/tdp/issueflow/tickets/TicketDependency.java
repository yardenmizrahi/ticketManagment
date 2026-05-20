package com.att.tdp.issueflow.tickets;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "ticket_dependencies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blocked_ticket_id", "blocker_ticket_id"})
)
public class TicketDependency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_ticket_id", nullable = false)
    private Ticket blockedTicket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_ticket_id", nullable = false)
    private Ticket blockerTicket;
}
