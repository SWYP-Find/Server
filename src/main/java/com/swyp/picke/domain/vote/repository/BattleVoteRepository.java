package com.swyp.picke.domain.vote.repository;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.vote.entity.BattleVote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BattleVoteRepository extends JpaRepository<BattleVote, Long> {

    List<BattleVote> findAllByBattle(Battle battle);

    Optional<BattleVote> findByBattleIdAndUserId(Long battleId, Long userId);

    @Query("SELECT v FROM BattleVote v LEFT JOIN FETCH v.postVoteOption WHERE v.battle.id = :battleId AND v.user.id = :userId")
    Optional<BattleVote> findByBattleIdAndUserIdWithOption(@Param("battleId") Long battleId, @Param("userId") Long userId);

    @Query("SELECT v FROM BattleVote v JOIN FETCH v.battle LEFT JOIN FETCH v.postVoteOption " +
            "WHERE v.user.id = :userId AND v.battle.id IN :battleIds")
    List<BattleVote> findByUserIdAndBattleIdInWithPostVoteOption(
            @Param("userId") Long userId,
            @Param("battleIds") List<Long> battleIds
    );

    Optional<BattleVote> findByBattleAndUser(Battle battle, User user);

    long countByBattle(Battle battle);

    long countByBattleAndPreVoteOption(Battle battle, BattleOption preVoteOption);

    long countByBattleAndPostVoteOption(Battle battle, BattleOption postVoteOption);

    long countByBattleAndPostVoteOptionIsNotNull(Battle battle);

    Optional<BattleVote> findTopByBattleOrderByUpdatedAtDesc(Battle battle);

    // 내 배틀 기록: 사후투표까지 완료한 기록만 노출
    @Query("SELECT v FROM BattleVote v JOIN FETCH v.battle JOIN FETCH v.preVoteOption JOIN FETCH v.postVoteOption " +
           "WHERE v.user.id = :userId AND v.postVoteOption IS NOT NULL ORDER BY v.createdAt DESC")
    List<BattleVote> findByUserIdAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT v FROM BattleVote v JOIN FETCH v.battle JOIN FETCH v.preVoteOption JOIN FETCH v.postVoteOption " +
           "WHERE v.user.id = :userId AND v.preVoteOption.displayOrder = :displayOrder AND v.postVoteOption IS NOT NULL ORDER BY v.createdAt DESC")
    List<BattleVote> findByUserIdAndPreVoteOptionDisplayOrderAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(
            @Param("userId") Long userId, @Param("displayOrder") Integer displayOrder, Pageable pageable);

    @Query("SELECT v FROM BattleVote v JOIN FETCH v.battle JOIN FETCH v.preVoteOption JOIN FETCH v.postVoteOption " +
           "WHERE v.user.id = :userId AND v.preVoteOption.displayOrder <> :displayOrder AND v.postVoteOption IS NOT NULL ORDER BY v.createdAt DESC")
    List<BattleVote> findByUserIdAndPreVoteOptionDisplayOrderNotAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(
            @Param("userId") Long userId, @Param("displayOrder") Integer displayOrder, Pageable pageable);

    @Query("SELECT COUNT(v) FROM BattleVote v WHERE v.user.id = :userId AND v.postVoteOption IS NOT NULL")
    long countByUserIdAndPostVoteOptionIsNotNull(@Param("userId") Long userId);

    @Query("SELECT COUNT(v) FROM BattleVote v WHERE v.user.id = :userId AND v.preVoteOption.displayOrder = :displayOrder AND v.postVoteOption IS NOT NULL")
    long countByUserIdAndPreVoteOptionDisplayOrderAndPostVoteOptionIsNotNull(@Param("userId") Long userId, @Param("displayOrder") Integer displayOrder);

    @Query("SELECT COUNT(v) FROM BattleVote v WHERE v.user.id = :userId AND v.preVoteOption.displayOrder <> :displayOrder AND v.postVoteOption IS NOT NULL")
    long countByUserIdAndPreVoteOptionDisplayOrderNotAndPostVoteOptionIsNotNull(@Param("userId") Long userId, @Param("displayOrder") Integer displayOrder);

    // countTotalParticipation 등 "사전투표만 해도 참여로 집계"하는 통계용 — postVoteOption 필터 없음
    long countByUserId(Long userId);

    // 오늘의 배틀 무료 참여(일 1회) 제한 체크용: 유저가 오늘 날짜인 배틀에 이미 참여했는지 확인
    boolean existsByUserIdAndBattle_TargetDate(Long userId, LocalDate targetDate);

    @Query("SELECT COUNT(v) FROM BattleVote v WHERE v.user.id = :userId " +
            "AND v.postVoteOption IS NOT NULL " +
            "AND v.preVoteOption <> v.postVoteOption")
    long countOpinionChangesByUserId(@Param("userId") Long userId);

    List<BattleVote> findByUserId(Long userId);

    // MypageService: 철학자 유형 산출용 - 최초 N개 투표 조회 (생성순)
    @Query("SELECT v FROM BattleVote v JOIN FETCH v.battle WHERE v.user.id = :userId ORDER BY v.createdAt ASC")
    List<BattleVote> findByUserIdOrderByCreatedAtAsc(@Param("userId") Long userId, Pageable pageable);

    // 추천용: 유저가 참여한 배틀 ID 조회
    @Query("SELECT v.battle.id FROM BattleVote v WHERE v.user.id = :userId")
    List<Long> findParticipatedBattleIdsByUserId(@Param("userId") Long userId);

    // 추천용: 특정 배틀에 참여한 유저 ID 조회
    @Query("SELECT DISTINCT v.user.id FROM BattleVote v WHERE v.battle.id IN :battleIds")
    List<Long> findUserIdsByBattleIds(@Param("battleIds") List<Long> battleIds);

    // 추천용: 특정 유저들이 참여한 배틀 ID 조회
    @Query("SELECT v.battle.id FROM BattleVote v WHERE v.user.id IN :userIds")
    List<Long> findParticipatedBattleIdsByUserIds(@Param("userIds") List<Long> userIds);
}
