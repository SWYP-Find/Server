package com.swyp.app.domain.perspective.repository;

import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PerspectiveRepository extends JpaRepository<Perspective, UUID> {

    boolean existsByBattleIdAndUserId(UUID battleId, Long userId);

    Optional<Perspective> findByBattleIdAndUserId(UUID battleId, Long userId);

    List<Perspective> findByBattleIdAndStatusOrderByCreatedAtDesc(UUID battleId, PerspectiveStatus status, Pageable pageable);

    List<Perspective> findByBattleIdAndStatusAndCreatedAtBeforeOrderByCreatedAtDesc(UUID battleId, PerspectiveStatus status, LocalDateTime cursor, Pageable pageable);

    List<Perspective> findByBattleIdAndOptionIdAndStatusOrderByCreatedAtDesc(UUID battleId, UUID optionId, PerspectiveStatus status, Pageable pageable);

    List<Perspective> findByBattleIdAndOptionIdAndStatusAndCreatedAtBeforeOrderByCreatedAtDesc(UUID battleId, UUID optionId, PerspectiveStatus status, LocalDateTime cursor, Pageable pageable);
}
