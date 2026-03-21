package com.swyp.app.domain.perspective.repository;

import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PerspectiveCommentRepository extends JpaRepository<PerspectiveComment, UUID> {

    List<PerspectiveComment> findByPerspectiveOrderByCreatedAtDesc(Perspective perspective, Pageable pageable);

    List<PerspectiveComment> findByPerspectiveAndCreatedAtBeforeOrderByCreatedAtDesc(Perspective perspective, LocalDateTime cursor, Pageable pageable);

    // MypageService: 사용자 댓글 활동 조회 (offset 페이지네이션)
    @Query("SELECT c FROM PerspectiveComment c JOIN FETCH c.perspective WHERE c.userId = :userId ORDER BY c.createdAt DESC")
    List<PerspectiveComment> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    long countByUserId(Long userId);
}
