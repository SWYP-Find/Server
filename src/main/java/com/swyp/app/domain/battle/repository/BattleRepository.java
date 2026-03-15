package com.swyp.app.domain.battle.repository;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.enums.BattleStatus;
import com.swyp.app.domain.battle.enums.BattleType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BattleRepository extends JpaRepository<Battle, UUID> {

    // 1. EDITOR PICK
    @Query("SELECT battle FROM Battle battle " +
            "WHERE battle.isEditorPick = true AND battle.status = :status " +
            "AND battle.deletedAt IS NULL " +
            "ORDER BY battle.createdAt DESC")
    List<Battle> findEditorPicks(@Param("status") BattleStatus status, Pageable pageable);

    // 2. 지금 뜨는 배틀
    @Query("SELECT battle FROM Battle battle JOIN Vote vote ON vote.battle = battle " +
            "WHERE vote.createdAt >= :yesterday AND battle.status = 'PUBLISHED' " +
            "AND battle.deletedAt IS NULL " +
            "GROUP BY battle ORDER BY COUNT(vote) DESC")
    List<Battle> findTrendingBattles(@Param("yesterday") LocalDateTime yesterday, Pageable pageable);

    // 3. Best 배틀
    @Query("SELECT battle FROM Battle battle " +
            "WHERE battle.status = 'PUBLISHED' AND battle.deletedAt IS NULL " +
            "ORDER BY (battle.totalParticipantsCount + (battle.commentCount * 5)) DESC")
    List<Battle> findBestBattles(Pageable pageable);

    // 4. 오늘의 Pické (단일 타입)
    @Query("SELECT battle FROM Battle battle " +
            "WHERE battle.type = :type AND battle.targetDate = :today " +
            "AND battle.status = 'PUBLISHED' AND battle.deletedAt IS NULL")
    List<Battle> findTodayPicks(@Param("type") BattleType type, @Param("today") LocalDate today);

    // 5. 새로운 배틀
    @Query("SELECT battle FROM Battle battle " +
            "WHERE battle.id NOT IN :excludeIds AND battle.status = 'PUBLISHED' " +
            "AND battle.deletedAt IS NULL " +
            "ORDER BY battle.createdAt DESC")
    List<Battle> findNewBattlesExcluding(@Param("excludeIds") List<UUID> excludeIds, Pageable pageable);

    // 기본 조회용
    List<Battle> findByTargetDateAndStatusAndDeletedAtIsNull(LocalDate date, BattleStatus status);
}