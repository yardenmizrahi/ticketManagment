package com.att.tdp.issueflow.tickets;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketDependencyRepository extends JpaRepository<TicketDependency, Long> {
    List<TicketDependency> findByBlockedTicketId(Long ticketId);

    Optional<TicketDependency> findByBlockedTicketIdAndBlockerTicketId(Long blockedTicketId, Long blockerTicketId);

    boolean existsByBlockedTicketIdAndBlockerTicketStatusNotAndBlockerTicketDeletedAtIsNull(Long blockedTicketId, TicketStatus status);
}
