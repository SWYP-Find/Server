package com.swyp.picke.domain.battle.repository;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleRepository extends JpaRepository<Battle, Long> {

    // 1. EDITOR PICK
    @Query("SELECT battle FROM Battle battle " +
            "WHERE battle.isEditorPick = true AND battle.status = :status " +
            "AND battle.deletedAt IS NULL " +
            "ORDER BY battle.createdAt DESC")
    List<Battle> findEditorPicks(@Param("status") BattleStatus status, Pageable pageable);

    // 2. 지금 뜨는 배틀
    @Query("SELECT battle FROM Battle battle JOIN BattleVote vote ON vote.battle = battle " +
            "WHERE vote.createdAt >= :yesterday " +
            "AND battle.status = 'PUBLISHED' AND battle.deletedAt IS NULL " +
            "GROUP BY battle ORDER BY COUNT(vote) DESC")
    List<Battle> findTrendingBattles(@Param("yesterday") LocalDateTime yesterday, Pageable pageable);

    // 3. Best 배틀
    @Query("SELECT battle FROM Battle battle " +
            "WHERE battle.status = 'PUBLISHED' AND battle.deletedAt IS NULL " +
            "ORDER BY (battle.totalParticipantsCount + (battle.commentCount * 5)) DESC")
    List<Battle> findBestBattles(Pageable pageable);

    // 4. 오늘의 Pické
    @Query("SELECT battle FROM Battle battle " +
            "WHERE battle.targetDate = :today " +
            "AND battle.status = 'PUBLISHED' AND battle.deletedAt IS NULL")
    List<Battle> findTodayPicks(@Param("today") LocalDate today, Pageable pageable);

    // 5. 새로운 배틀
    @Query("SELECT battle FROM Battle battle " +
            "WHERE battle.status = 'PUBLISHED' " +
            "AND battle.deletedAt IS NULL " +
            "AND (battle.targetDate IS NULL OR battle.targetDate < :today) " +
            "ORDER BY CASE WHEN battle.targetDate IS NULL THEN 0 ELSE 1 END, battle.targetDate ASC, battle.createdAt ASC")
    List<Battle> findAutoAssignableTodayPicks(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT battle FROM Battle battle " +
            "WHERE battle.id NOT IN :excludeIds " +
            "AND battle.status = 'PUBLISHED' AND battle.deletedAt IS NULL " +
            "ORDER BY battle.createdAt DESC")
    List<Battle> findNewBattlesExcluding(@Param("excludeIds") List<Long> excludeIds, Pageable pageable);

    // 6. 전체 배틀 목록 조회
    Page<Battle> findByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);
    Page<Battle> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(BattleStatus status, Pageable pageable);
    List<Battle> findByStatusAndDeletedAtIsNull(BattleStatus status);

    // 기본 조회용
    List<Battle> findByTargetDateAndStatusAndDeletedAtIsNull(LocalDate date, BattleStatus status);

    // 주간 배치: 특정 기간(targetDate BETWEEN from AND to)의 배틀 조회
    List<Battle> findByTargetDateBetweenAndStatusAndDeletedAtIsNull(LocalDate from, LocalDate to, BattleStatus status);

    // 탐색 탭: 전체 배틀 검색
    @Query("SELECT b FROM Battle b WHERE b.status = 'PUBLISHED' AND b.deletedAt IS NULL")
    List<Battle> searchAll(Pageable pageable);

    @Query("SELECT COUNT(b) FROM Battle b WHERE b.status = 'PUBLISHED' AND b.deletedAt IS NULL")
    long countSearchAll();

    // 탐색 탭: 카테고리 태그 필터 배틀 검색
    @Query("SELECT DISTINCT b FROM Battle b JOIN BattleTag bt ON bt.battle = b JOIN bt.tag t " +
            "WHERE t.type = 'CATEGORY' AND t.name = :categoryName " +
            "AND b.status = 'PUBLISHED' AND b.deletedAt IS NULL")
    List<Battle> searchByCategory(@Param("categoryName") String categoryName, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT b) FROM Battle b JOIN BattleTag bt ON bt.battle = b JOIN bt.tag t " +
            "WHERE t.type = 'CATEGORY' AND t.name = :categoryName " +
            "AND b.status = 'PUBLISHED' AND b.deletedAt IS NULL")
    long countSearchByCategory(@Param("categoryName") String categoryName);

    // 추천 폴백용: 전체 배틀 대상 인기 점수순 조회 (철학자 유형 로직 미구현 시 사용)
    // Score = V*1.0 + C*1.5 + Vw*0.2
    @Query("SELECT b FROM Battle b " +
            "WHERE b.id NOT IN :excludeBattleIds " +
            "AND b.status = 'PUBLISHED' AND b.deletedAt IS NULL " +
            "ORDER BY (b.totalParticipantsCount * 1.0 + b.commentCount * 1.5 + b.viewCount * 0.2) DESC")
    List<Battle> findPopularBattlesExcluding(
            @Param("excludeBattleIds") List<Long> excludeBattleIds,
            Pageable pageable
    );

    // 추천용: 특정 유저들이 참여한 배틀 중 이미 참여한 배틀 제외하고 인기 점수순 조회
    // Score = V*1.0 + C*1.5 + Vw*0.2 (R은 추후 반영 예정)
    @Query("SELECT b FROM Battle b " +
            "WHERE b.id IN :candidateBattleIds " +
            "AND b.id NOT IN :excludeBattleIds " +
            "AND b.status = 'PUBLISHED' AND b.deletedAt IS NULL " +
            "ORDER BY (b.totalParticipantsCount * 1.0 + b.commentCount * 1.5 + b.viewCount * 0.2) DESC")
    List<Battle> findRecommendedBattles(
            @Param("candidateBattleIds") List<Long> candidateBattleIds,
            @Param("excludeBattleIds") List<Long> excludeBattleIds,
            Pageable pageable
    );
}
