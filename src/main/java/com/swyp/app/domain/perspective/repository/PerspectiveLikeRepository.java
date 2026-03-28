package com.swyp.app.domain.perspective.repository;

import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerspectiveLikeRepository extends JpaRepository<PerspectiveLike, Long> {

    boolean existsByPerspectiveAndUserId(Perspective perspective, Long userId);

    Optional<PerspectiveLike> findByPerspectiveAndUserId(Perspective perspective, Long userId);

    long countByPerspective(Perspective perspective);

    // MypageService: 사용자 좋아요 활동 조회 (offset 페이지네이션)
    @Query("SELECT l FROM PerspectiveLike l JOIN FETCH l.perspective WHERE l.userId = :userId ORDER BY l.createdAt DESC")
    List<PerspectiveLike> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    long countByUserId(Long userId);
}
