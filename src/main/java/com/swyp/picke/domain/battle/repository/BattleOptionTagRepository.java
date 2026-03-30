package com.swyp.picke.domain.battle.repository;

import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.entity.BattleOptionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BattleOptionTagRepository extends JpaRepository<BattleOptionTag, Long> {
    List<BattleOptionTag> findByBattleOption(BattleOption battleOption);
}