package com.swyp.app.domain.battle.repository;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleTag;
import com.swyp.app.domain.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BattleTagRepository extends JpaRepository<BattleTag, Long> {
    List<BattleTag> findByBattle(Battle battle);
    void deleteByBattle(Battle battle);
    boolean existsByTag(Tag tag);
    // N+1 방지를 위해 Tag까지 한 번에 가져오는 쿼리
    @Query("SELECT bt FROM BattleTag bt JOIN FETCH bt.tag WHERE bt.battle IN :battles")
    List<BattleTag> findByBattleIn(@Param("battles") List<Battle> battles);
    // MypageService (recap): 여러 배틀의 태그를 한번에 조회
    @Query("SELECT bt FROM BattleTag bt JOIN FETCH bt.tag WHERE bt.battle.id IN :battleIds")
    List<BattleTag> findByBattleIdIn(@Param("battleIds") List<Long> battleIds);
}