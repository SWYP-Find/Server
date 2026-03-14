package com.swyp.app.domain.perspective.repository;

import com.swyp.app.domain.perspective.entity.Perspective;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PerspectiveRepository extends JpaRepository<Perspective, UUID> {

    boolean existsByBattleIdAndUserId(UUID battleId, Long userId);

    List<Perspective> findByBattleIdOrderByCreatedAtDesc(UUID battleId, Pageable pageable);

    List<Perspective> findByBattleIdAndCreatedAtBeforeOrderByCreatedAtDesc(UUID battleId, LocalDateTime cursor, Pageable pageable);

    // TODO: BattleOption 병합 후 optionId 필터 쿼리 추가
}
