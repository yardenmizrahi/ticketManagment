package com.att.tdp.issueflow.comments;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.comments.CommentDtos.*;
import com.att.tdp.issueflow.common.Require;
import com.att.tdp.issueflow.tickets.Ticket;
import com.att.tdp.issueflow.tickets.TicketService;
import com.att.tdp.issueflow.users.User;
import com.att.tdp.issueflow.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentMentionRepository mentionRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final MentionService mentionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CommentResponse> findByTicket(Long ticketId) {
        ticketService.loadActive(ticketId);
        return commentRepository.findByTicketIdOrderByIdAsc(ticketId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CommentResponse create(Long ticketId, CreateCommentRequest request, User actor) {
        Ticket ticket = ticketService.loadActive(ticketId);
        User author = Require.found(userRepository.findById(request.authorId()), "Author user not found");
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(request.content());
        Comment saved = commentRepository.save(comment);
        mentionService.replaceMentions(saved, mentionService.resolveMentionedUsers(saved.getContent()));
        auditService.user("CREATE", "COMMENT", saved.getId(), actor);
        return toResponse(saved);
    }

    @Transactional
    public void update(Long ticketId, Long commentId, UpdateCommentRequest request, User actor) {
        ticketService.loadActive(ticketId);
        Comment comment = Require.found(commentRepository.findByIdAndTicketId(commentId, ticketId), "Comment not found");
        comment.setContent(request.content());
        mentionService.replaceMentions(comment, mentionService.resolveMentionedUsers(comment.getContent()));
        auditService.user("UPDATE", "COMMENT", commentId, actor);
    }

    @Transactional
    public void delete(Long ticketId, Long commentId, User actor) {
        ticketService.loadActive(ticketId);
        Comment comment = Require.found(commentRepository.findByIdAndTicketId(commentId, ticketId), "Comment not found");
        mentionRepository.deleteByCommentId(commentId);
        commentRepository.delete(comment);
        auditService.user("DELETE", "COMMENT", commentId, actor);
    }

    @Transactional(readOnly = true)
    public MentionsPageResponse findMentions(Long userId, int page, int pageSize) {
        Require.found(userRepository.findById(userId), "User not found");
        int zeroBased = Math.max(page, 1) - 1;
        Page<CommentMention> mentions = mentionRepository.findByUserIdOrderByCommentCreatedAtDescIdDesc(
                userId, PageRequest.of(zeroBased, Math.max(1, Math.min(pageSize, 100))));
        return new MentionsPageResponse(mentions.map(CommentMention::getComment).map(this::toResponse).toList(),
                mentions.getTotalElements(), page);
    }

    private CommentResponse toResponse(Comment comment) {
        List<MentionedUserResponse> mentionedUsers = mentionRepository.findByCommentId(comment.getId()).stream()
                .map(CommentMention::getUser)
                .map(user -> new MentionedUserResponse(user.getId(), user.getUsername(), user.getFullName()))
                .toList();
        return new CommentResponse(comment.getId(), comment.getTicket().getId(), comment.getAuthor().getId(),
                comment.getContent(), mentionedUsers);
    }
}
