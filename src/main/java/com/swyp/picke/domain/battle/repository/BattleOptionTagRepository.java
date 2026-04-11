package com.swyp.picke.domain.battle.repository;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.entity.BattleOptionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BattleOptionTagRepository extends JpaRepository<BattleOptionTag, Long> {
    List<BattleOptionTag> findByBattleOption(BattleOption battleOption);

    @Query("SELECT bot FROM BattleOptionTag bot JOIN FETCH bot.tag WHERE bot.battleOption.battle = :battle")
    List<BattleOptionTag> findByBattleWithTags(@Param("battle") Battle battle);

    @Query("SELECT bot FROM BattleOptionTag bot JOIN FETCH bot.tag WHERE bot.battleOption.id IN :optionIds")
    List<BattleOptionTag> findByBattleOptionIdIn(@Param("optionIds") List<Long> optionIds);
}