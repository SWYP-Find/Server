package com.swyp.app.domain.vote.repository;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    Optional<Vote> findByBattleAndUserId(Battle battle, Long userId);

    long countByBattle(Battle battle);

    long countByBattleAndPreVoteOption(Battle battle, BattleOption preVoteOption);

    Optional<Vote> findTopByBattleOrderByUpdatedAtDesc(Battle battle);
}