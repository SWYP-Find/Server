package com.swyp.app.domain.perspective.repository;

import com.swyp.app.domain.perspective.entity.CommentReport;
import com.swyp.app.domain.perspective.entity.PerspectiveComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    boolean existsByCommentAndUserId(PerspectiveComment comment, Long userId);

    long countByComment(PerspectiveComment comment);
}
