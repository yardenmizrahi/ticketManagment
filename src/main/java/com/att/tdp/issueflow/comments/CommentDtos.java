package com.att.tdp.issueflow.comments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class CommentDtos {
    private CommentDtos() {
    }

    public record CreateCommentRequest(@NotNull Long authorId, @NotBlank String content) {
    }

    public record UpdateCommentRequest(@NotBlank String content) {
    }

    public record MentionedUserResponse(Long id, String username, String fullName) {
    }

    public record CommentResponse(
            Long id,
            Long ticketId,
            Long authorId,
            String content,
            List<MentionedUserResponse> mentionedUsers
    ) {
    }

    public record MentionsPageResponse(List<CommentResponse> data, long total, int page) {
    }
}
