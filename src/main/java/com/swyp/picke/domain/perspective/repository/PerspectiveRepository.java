package com.swyp.picke.domain.perspective.repository;

import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * perspectives.class_id: 일반 배틀 의견은 NULL, 클래스 내 의견은 클래스 id.
 * 배틀 단위 목록 조회는 Classroom 파라미터로 스코프를 명시한다 (null → class_id IS NULL).
 */
public interface PerspectiveRepository extends JpaRepository<Perspective, Long> {

    List<Perspective> findByBattleIdAndUserIdAndClassroomOrderByCreatedAtDesc(Long battleId, Long userId, Classroom classroom);

    // 클래스 참여 완료 판정(댓글 필수): 본인 최상위 의견 존재 여부
    boolean existsByBattleIdAndUserIdAndClassroomIdAndStatus(Long battleId, Long userId, Long classroomId, PerspectiveStatus status);

    // 마이페이지: 일반 배틀 의견만
    List<Perspective> findByUserIdAndClassroomIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndClassroomIsNull(Long userId);

    List<Perspective> findByBattleIdAndClassroomAndStatusOrderByCreatedAtDesc(Long battleId, Classroom classroom, PerspectiveStatus status, Pageable pageable);

    List<Perspective> findByBattleIdAndClassroomAndStatusAndCreatedAtBeforeOrderByCreatedAtDesc(Long battleId, Classroom classroom, PerspectiveStatus status, LocalDateTime cursor, Pageable pageable);

    List<Perspective> findByBattleIdAndClassroomAndOptionIdAndStatusOrderByCreatedAtDesc(Long battleId, Classroom classroom, Long optionId, PerspectiveStatus status, Pageable pageable);

    List<Perspective> findByBattleIdAndClassroomAndOptionIdAndStatusAndCreatedAtBeforeOrderByCreatedAtDesc(Long battleId, Classroom classroom, Long optionId, PerspectiveStatus status, LocalDateTime cursor, Pageable pageable);

    List<Perspective> findByBattleIdAndClassroomAndStatusOrderByLikeCountDescCreatedAtDesc(Long battleId, Classroom classroom, PerspectiveStatus status, Pageable pageable);

    List<Perspective> findByBattleIdAndClassroomAndOptionIdAndStatusOrderByLikeCountDescCreatedAtDesc(Long battleId, Classroom classroom, Long optionId, PerspectiveStatus status, Pageable pageable);

    // 베댓 보상 배치: 일반 배틀 의견만
    List<Perspective> findByBattleIdAndClassroomIsNullAndStatusOrderByLikeCountDescCreatedAtDesc(Long battleId, PerspectiveStatus status, Pageable pageable);
}
