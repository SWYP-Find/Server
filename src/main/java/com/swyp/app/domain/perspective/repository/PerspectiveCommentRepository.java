package com.swyp.app.domain.perspective.repository;

import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PerspectiveCommentRepository extends JpaRepository<PerspectiveComment, Long> {

    List<PerspectiveComment> findByPerspectiveOrderByCreatedAtDesc(Perspective perspective, Pageable pageable);

    List<PerspectiveComment> findByPerspectiveAndCreatedAtBeforeOrderByCreatedAtDesc(Perspective perspective, LocalDateTime cursor, Pageable pageable);
}
