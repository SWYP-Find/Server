package com.swyp.app.domain.battle.repository;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleTag;
import com.swyp.app.domain.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BattleTagRepository extends JpaRepository<BattleTag, UUID> {
    List<BattleTag> findByBattle(Battle battle);
    @Query("SELECT battleTag FROM BattleTag battleTag JOIN FETCH battleTag.tag WHERE battleTag.battle IN :battles")
    List<BattleTag> findByBattleIn(@Param("battles") List<Battle> battles);
    void deleteByBattle(Battle battle);
    boolean existsByTag(Tag tag);
}
