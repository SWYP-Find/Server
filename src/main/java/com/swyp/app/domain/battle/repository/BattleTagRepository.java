package com.swyp.app.domain.battle.repository;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleTag;
import com.swyp.app.domain.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BattleTagRepository extends JpaRepository<BattleTag, Long> {
    List<BattleTag> findByBattle(Battle battle);
    void deleteByBattle(Battle battle);
    boolean existsByTag(Tag tag);
}