package com.swyp.picke.domain.perspective.repository;

import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * perspective_comments.class_id 는 부모 의견의 class_id 를 복사한다.
 * 의견(perspective) 기준 조회는 부모가 이미 스코프를 가지므로 추가 조건 불필요, 유저·배틀 단위 집계는 일반(NULL)만.
 */
public interface PerspectiveCommentRepository extends JpaRepository<PerspectiveComment, Long> {

    List<PerspectiveComment> findByPerspective(Perspective perspective);

    List<PerspectiveComment> findByPerspectiveOrderByCreatedAtDesc(Perspective perspective, Pageable pageable);

    List<PerspectiveComment> findByPerspectiveAndCreatedAtBeforeOrderByCreatedAtDesc(Perspective perspective, LocalDateTime cursor, Pageable pageable);

    @Query("SELECT c FROM PerspectiveComment c JOIN FETCH c.perspective WHERE c.user.id = :userId AND c.classroom IS NULL ORDER BY c.createdAt DESC")
    List<PerspectiveComment> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    long countByUserIdAndClassroomIsNull(Long userId);

    void deleteAllByPerspective(Perspective perspective);

    // 클래스 결과·리포트용: 클래스 안의 공개 댓글 전체 / 본인 댓글 (시간순)
    List<PerspectiveComment> findByClassroomIdAndHiddenFalseOrderByCreatedAtAsc(Long classroomId);

    List<PerspectiveComment> findByClassroomIdAndUserIdAndHiddenFalseOrderByCreatedAtAsc(Long classroomId, Long userId);

    @Modifying(flushAutomatically = true)
    @Query("update PerspectiveComment c set c.hidden = true where c.user.id = :userId and c.hidden = false")
    int hideAllByUserId(@Param("userId") Long userId);

    @Query("""
            select c from PerspectiveComment c
            join fetch c.perspective p
            where c.classroom.id = :classId and c.user.id = :userId
              and c.hidden = false and p.status = com.swyp.picke.domain.perspective.enums.PerspectiveStatus.PUBLISHED
            order by c.createdAt desc, c.id desc
            """)
    List<PerspectiveComment> findVisibleByClassroomIdAndUserIdOrderByCreatedAtDesc(
            @Param("classId") Long classId, @Param("userId") Long userId, Pageable pageable);

    @Query("""
            select c from PerspectiveComment c
            join fetch c.perspective p
            where c.classroom.id = :classId and c.user.id = :userId
              and c.hidden = false and p.status = com.swyp.picke.domain.perspective.enums.PerspectiveStatus.PUBLISHED
              and c.createdAt < :cursor
            order by c.createdAt desc, c.id desc
            """)
    List<PerspectiveComment> findVisibleByClassroomIdAndUserIdBeforeOrderByCreatedAtDesc(
            @Param("classId") Long classId, @Param("userId") Long userId,
            @Param("cursor") LocalDateTime cursor, Pageable pageable);

    @Query("""
            select count(c) from PerspectiveComment c
            join c.perspective p
            where c.classroom.id = :classId and c.user.id = :userId
              and c.hidden = false and p.status = com.swyp.picke.domain.perspective.enums.PerspectiveStatus.PUBLISHED
            """)
    long countVisibleByClassroomIdAndUserId(@Param("classId") Long classId, @Param("userId") Long userId);

    @Query("SELECT c FROM PerspectiveComment c WHERE c.perspective.battle.id = :battleId AND c.classroom IS NULL AND c.hidden = false AND c.likeCount >= :minLikeCount ORDER BY c.likeCount DESC")
    List<PerspectiveComment> findTopCommentsByBattleId(@Param("battleId") Long battleId, @Param("minLikeCount") int minLikeCount, Pageable pageable);
}
