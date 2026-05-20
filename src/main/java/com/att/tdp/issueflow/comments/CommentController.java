package com.att.tdp.issueflow.comments;

import com.att.tdp.issueflow.auth.CurrentUser;
import com.att.tdp.issueflow.comments.CommentDtos.CommentResponse;
import com.att.tdp.issueflow.comments.CommentDtos.CreateCommentRequest;
import com.att.tdp.issueflow.comments.CommentDtos.UpdateCommentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<CommentResponse> findByTicket(@PathVariable Long ticketId) {
        return commentService.findByTicket(ticketId);
    }

    @PostMapping
    public CommentResponse create(@PathVariable Long ticketId, @Valid @RequestBody CreateCommentRequest request) {
        return commentService.create(ticketId, request, currentUser.get());
    }

    @PatchMapping("/{commentId}")
    public void update(
            @PathVariable Long ticketId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        commentService.update(ticketId, commentId, request, currentUser.get());
    }

    @DeleteMapping("/{commentId}")
    public void delete(@PathVariable Long ticketId, @PathVariable Long commentId) {
        commentService.delete(ticketId, commentId, currentUser.get());
    }
}
