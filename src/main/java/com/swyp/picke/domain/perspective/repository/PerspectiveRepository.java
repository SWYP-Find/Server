package com.swyp.picke.domain.perspective.repository;

import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PerspectiveRepository extends JpaRepository<Perspective, Long> {

    boolean existsByBattleIdAndUserId(Long battleId, Long userId);

    Optional<Perspective> findByBattleIdAndUserId(Long battleId, Long userId);

    List<Perspective> findByBattleIdAndStatusOrderByCreatedAtDesc(Long battleId, PerspectiveStatus status, Pageable pageable);

    List<Perspective> findByBattleIdAndStatusAndCreatedAtBeforeOrderByCreatedAtDesc(Long battleId, PerspectiveStatus status, LocalDateTime cursor, Pageable pageable);

    List<Perspective> findByBattleIdAndOptionIdAndStatusOrderByCreatedAtDesc(Long battleId, Long optionId, PerspectiveStatus status, Pageable pageable);

    List<Perspective> findByBattleIdAndOptionIdAndStatusAndCreatedAtBeforeOrderByCreatedAtDesc(Long battleId, Long optionId, PerspectiveStatus status, LocalDateTime cursor, Pageable pageable);

    List<Perspective> findByBattleIdAndStatusOrderByLikeCountDescCreatedAtDesc(Long battleId, PerspectiveStatus status, Pageable pageable);

    List<Perspective> findByBattleIdAndOptionIdAndStatusOrderByLikeCountDescCreatedAtDesc(Long battleId, Long optionId, PerspectiveStatus status, Pageable pageable);
}
