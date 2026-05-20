package com.att.tdp.issueflow.comments;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentMentionRepository extends JpaRepository<CommentMention, Long> {
    List<CommentMention> findByCommentId(Long commentId);

    void deleteByCommentId(Long commentId);

    Page<CommentMention> findByUserIdOrderByCommentCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
