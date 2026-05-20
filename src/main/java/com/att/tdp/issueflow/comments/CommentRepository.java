package com.att.tdp.issueflow.comments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTicketIdOrderByIdAsc(Long ticketId);

    Optional<Comment> findByIdAndTicketId(Long id, Long ticketId);
}
