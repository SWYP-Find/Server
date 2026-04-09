package com.swyp.picke.domain.perspective.repository;

import com.swyp.picke.domain.perspective.entity.CommentReport;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    boolean existsByCommentAndUserId(PerspectiveComment comment, Long userId);

    long countByComment(PerspectiveComment comment);
}
