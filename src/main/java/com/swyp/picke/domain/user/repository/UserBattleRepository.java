package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserBattle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBattleRepository extends JpaRepository<UserBattle, Long> {
    // 특정 유저가 특정 배틀에 참여한 기록 찾기
    Optional<UserBattle> findByUserAndBattle(User user, Battle battle);
}
