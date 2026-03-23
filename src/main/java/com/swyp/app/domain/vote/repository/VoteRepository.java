package com.swyp.app.domain.vote.repository;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    // ScenarioService : Battle 엔티티 조회 없이 ID만으로 투표 내역 확인
    Optional<Vote> findByBattleIdAndUserId(Long battleId, Long userId);

    // VoteService : 이미 조회된 Battle 엔티티를 활용하여 투표 내역 확인
    Optional<Vote> findByBattleAndUserId(Battle battle, Long userId);

    long countByBattle(Battle battle);

    long countByBattleAndPreVoteOption(Battle battle, BattleOption preVoteOption);

    Optional<Vote> findTopByBattleOrderByUpdatedAtDesc(Battle battle);
}