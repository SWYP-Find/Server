package com.swyp.picke.domain.vote.repository;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.vote.entity.QuizVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizVoteRepository extends JpaRepository<QuizVote, Long> {
    Optional<QuizVote> findByBattleAndUser(Battle battle, User user);
    long countByBattle(Battle battle);
    long countByBattleAndSelectedOption(Battle battle, BattleOption option);
}
