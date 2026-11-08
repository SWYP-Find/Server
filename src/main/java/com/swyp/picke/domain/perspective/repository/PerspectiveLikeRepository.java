package com.swyp.picke.domain.perspective.repository;

import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveLike;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.perspective.repository.projection.WeeklyPerspectiveLikeCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PerspectiveLikeRepository extends JpaRepository<PerspectiveLike, Long> {

    boolean existsByPerspectiveAndUserId(Perspective perspective, Long userId);

    Optional<PerspectiveLike> findByPerspectiveAndUserId(Perspective perspective, Long userId);

    long countByPerspective(Perspective perspective);

    @Query("SELECT l FROM PerspectiveLike l JOIN FETCH l.perspective WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    List<PerspectiveLike> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    long countByUserId(Long userId);

    @Query("""
            select count(l) from PerspectiveLike l
            where l.perspective.classroom.id = :classId
              and l.perspective.user.id = :userId
              and l.perspective.status = com.swyp.picke.domain.perspective.enums.PerspectiveStatus.PUBLISHED
            """)
    long countReceivedByUserInClass(@org.springframework.data.repository.query.Param("classId") Long classId,
                                    @org.springframework.data.repository.query.Param("userId") Long userId);

    // 주간 베스트 관점: 기간 안에 눌린 좋아요 수 기준. 취소된 좋아요는 행이 삭제되어 자동으로 빠진다
    @Query("""
            select p.id as perspectiveId, count(l) as likeCount
            from PerspectiveLike l
            join l.perspective p
            join p.battle b
            where l.createdAt >= :start
              and l.createdAt < :end
              and p.classroom is null
              and p.status = :perspectiveStatus
              and b.status = :battleStatus
            group by p.id, p.createdAt
            order by count(l) desc, p.createdAt desc, p.id desc
            """)
    List<WeeklyPerspectiveLikeCount> findWeeklyLikeRanking(@Param("start") LocalDateTime start,
                                                           @Param("end") LocalDateTime end,
                                                           @Param("perspectiveStatus") PerspectiveStatus perspectiveStatus,
                                                           @Param("battleStatus") BattleStatus battleStatus,
                                                           Pageable pageable);

    void deleteAllByPerspective(Perspective perspective);
}
