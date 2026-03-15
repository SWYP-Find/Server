package com.swyp.app.domain.perspective.repository;

import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PerspectiveCommentRepository extends JpaRepository<PerspectiveComment, UUID> {

    List<PerspectiveComment> findByPerspectiveOrderByCreatedAtDesc(Perspective perspective, Pageable pageable);

    List<PerspectiveComment> findByPerspectiveAndCreatedAtBeforeOrderByCreatedAtDesc(Perspective perspective, LocalDateTime cursor, Pageable pageable);
}
