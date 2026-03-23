package com.swyp.app.domain.battle.repository;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BattleOptionRepository extends JpaRepository<BattleOption, Long> {

    List<BattleOption> findByBattle(Battle battle);
    Optional<BattleOption> findByBattleAndLabel(Battle battle, BattleOptionLabel label);

}
