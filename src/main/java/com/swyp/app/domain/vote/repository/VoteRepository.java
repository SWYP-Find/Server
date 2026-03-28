package com.swyp.app.domain.vote.repository;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.vote.entity.Vote;
import com.swyp.app.domain.vote.enums.VoteStatus;
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

    long countByUserIdAndStatus(Long userId, VoteStatus status);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.user.id = :userId AND v.status = 'POST_VOTED' " +
           "AND v.preVoteOption <> v.postVoteOption")
    long countOpinionChangesByUserId(@Param("userId") Long userId);

    List<Vote> findByUserId(Long userId);

    // MypageService: 철학자 유형 산출용 - 최초 N개 투표 조회 (생성순)
    @Query("SELECT v FROM Vote v JOIN FETCH v.battle WHERE v.user.id = :userId ORDER BY v.createdAt ASC")
    List<Vote> findByUserIdOrderByCreatedAtAsc(@Param("userId") Long userId, Pageable pageable);
}
