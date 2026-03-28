package com.swyp.app.domain.perspective.repository;

import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerspectiveReportRepository extends JpaRepository<PerspectiveReport, Long> {

    boolean existsByPerspectiveAndUserId(Perspective perspective, Long userId);

    long countByPerspective(Perspective perspective);
}
