package com.swyp.app.domain.perspective.repository;

import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerspectiveLikeRepository extends JpaRepository<PerspectiveLike, Long> {

    boolean existsByPerspectiveAndUserId(Perspective perspective, Long userId);

    Optional<PerspectiveLike> findByPerspectiveAndUserId(Perspective perspective, Long userId);

    long countByPerspective(Perspective perspective);
}
