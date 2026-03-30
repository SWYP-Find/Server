package com.swyp.picke.domain.vote.repository;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.vote.entity.Vote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByBattleIdAndUserId(Long battleId, Long userId);

    Optional<Vote> findByBattleAndUser(Battle battle, User user);

    long countByBattle(Battle battle);

    long countByBattleAndPreVoteOption(Battle battle, BattleOption preVoteOption);

    Optional<Vote> findTopByBattleOrderByUpdatedAtDesc(Battle battle);

    @Query("SELECT v FROM Vote v JOIN FETCH v.battle JOIN FETCH v.preVoteOption " +
           "WHERE v.user.id = :userId ORDER BY v.createdAt DESC")
    List<Vote> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT v FROM Vote v JOIN FETCH v.battle JOIN FETCH v.preVoteOption " +
           "WHERE v.user.id = :userId AND v.preVoteOption.label = :label ORDER BY v.createdAt DESC")
    List<Vote> findByUserIdAndPreVoteOptionLabelOrderByCreatedAtDesc(
            @Param("userId") Long userId, @Param("label") BattleOptionLabel label, Pageable pageable);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.user.id = :userId AND v.preVoteOption.label = :label")
    long countByUserIdAndPreVoteOptionLabel(@Param("userId") Long userId, @Param("label") BattleOptionLabel label);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.user.id = :userId " +
            "AND v.postVoteOption IS NOT NULL " +
            "AND v.preVoteOption <> v.postVoteOption")
    long countOpinionChangesByUserId(@Param("userId") Long userId);

    List<Vote> findByUserId(Long userId);

    // MypageService: 철학자 유형 산출용 - 최초 N개 투표 조회 (생성순)
    @Query("SELECT v FROM Vote v JOIN FETCH v.battle WHERE v.user.id = :userId ORDER BY v.createdAt ASC")
    List<Vote> findByUserIdOrderByCreatedAtAsc(@Param("userId") Long userId, Pageable pageable);

    // 추천용: 유저가 참여한 배틀 ID 조회
    @Query("SELECT v.battle.id FROM Vote v WHERE v.user.id = :userId")
    List<Long> findParticipatedBattleIdsByUserId(@Param("userId") Long userId);

    // 추천용: 특정 배틀에 참여한 유저 ID 조회
    @Query("SELECT DISTINCT v.user.id FROM Vote v WHERE v.battle.id IN :battleIds")
    List<Long> findUserIdsByBattleIds(@Param("battleIds") List<Long> battleIds);

    // 추천용: 특정 유저들이 참여한 배틀 ID 조회
    @Query("SELECT v.battle.id FROM Vote v WHERE v.user.id IN :userIds")
    List<Long> findParticipatedBattleIdsByUserIds(@Param("userIds") List<Long> userIds);
}
