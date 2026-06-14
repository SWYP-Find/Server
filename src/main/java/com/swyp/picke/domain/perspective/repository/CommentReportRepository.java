package com.swyp.picke.domain.perspective.repository;

import com.swyp.picke.domain.perspective.entity.CommentReport;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    boolean existsByCommentAndUserId(PerspectiveComment comment, Long userId);

    long countByComment(PerspectiveComment comment);

    void deleteAllByCommentIn(List<PerspectiveComment> comments);

    void deleteAllByComment(PerspectiveComment comment);
}
