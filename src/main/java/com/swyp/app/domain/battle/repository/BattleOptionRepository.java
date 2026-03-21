package com.swyp.app.domain.battle.repository;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BattleOptionRepository extends JpaRepository<BattleOption, UUID> {

    List<BattleOption> findByBattle(Battle battle);
    @Query("SELECT battleOption FROM BattleOption battleOption WHERE battleOption.battle IN :battles ORDER BY battleOption.battle.id ASC, battleOption.label ASC")
    List<BattleOption> findByBattleInOrderByBattleIdAscLabelAsc(@Param("battles") List<Battle> battles);
    Optional<BattleOption> findByBattleAndLabel(Battle battle, BattleOptionLabel label);

}
