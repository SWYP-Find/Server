package com.swyp.picke.domain.battle.repository;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BattleOptionRepository extends JpaRepository<BattleOption, Long> {

    @Query("SELECT bo FROM BattleOption bo " +
            "WHERE bo.battle = :battle " +
            "ORDER BY COALESCE(bo.displayOrder, 9999), bo.label, bo.id")
    List<BattleOption> findByBattle(@Param("battle") Battle battle);

    Optional<BattleOption> findByBattleAndLabel(Battle battle, BattleOptionLabel label);

    @Query("SELECT bo FROM BattleOption bo " +
            "WHERE bo.battle IN :battles " +
            "ORDER BY bo.battle.id, COALESCE(bo.displayOrder, 9999), bo.label, bo.id")
    List<BattleOption> findByBattleIn(@Param("battles") List<Battle> battles);
}
