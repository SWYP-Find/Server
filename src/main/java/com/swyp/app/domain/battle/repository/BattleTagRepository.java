package com.swyp.app.domain.battle.repository;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BattleTagRepository extends JpaRepository<BattleTag, UUID> {

    List<BattleTag> findByBattle(Battle battle);
}
