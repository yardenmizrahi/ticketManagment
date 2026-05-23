package com.att.tdp.issueflow.tickets;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByIdAndDeletedAtIsNull(Long id);

    List<Ticket> findByProjectIdAndDeletedAtIsNullOrderByIdAsc(Long projectId);

    List<Ticket> findByProjectIdAndDeletedAtIsNotNullOrderByIdAsc(Long projectId);

    long countByProjectIdAndAssigneeIdAndStatusNotAndDeletedAtIsNull(Long projectId, Long assigneeId, TicketStatus status);

    boolean existsByProjectIdAndAssigneeIdAndDeletedAtIsNull(Long projectId, Long assigneeId);

    @Query("""
            select t from Ticket t
            where t.deletedAt is null
              and t.status <> com.att.tdp.issueflow.tickets.TicketStatus.DONE
              and t.dueDate is not null
              and t.dueDate < :now
            order by t.id asc
            """)
    List<Ticket> findOverdueUnresolved(Instant now);
}
