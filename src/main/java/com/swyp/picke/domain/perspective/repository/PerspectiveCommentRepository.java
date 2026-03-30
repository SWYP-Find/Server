package com.swyp.picke.domain.perspective.repository;

import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PerspectiveCommentRepository extends JpaRepository<PerspectiveComment, Long> {

    List<PerspectiveComment> findByPerspectiveOrderByCreatedAtDesc(Perspective perspective, Pageable pageable);

    List<PerspectiveComment> findByPerspectiveAndCreatedAtBeforeOrderByCreatedAtDesc(Perspective perspective, LocalDateTime cursor, Pageable pageable);

    @Query("SELECT c FROM PerspectiveComment c JOIN FETCH c.perspective WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
    List<PerspectiveComment> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    long countByUserId(Long userId);
}
