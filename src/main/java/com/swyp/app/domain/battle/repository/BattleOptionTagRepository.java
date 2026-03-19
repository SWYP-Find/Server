package com.swyp.app.domain.battle.repository;

import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.entity.BattleOptionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BattleOptionTagRepository extends JpaRepository<BattleOptionTag, UUID> {
    List<BattleOptionTag> findByBattleOption(BattleOption battleOption);
}