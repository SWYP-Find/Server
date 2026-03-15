package com.swyp.app.domain.tag.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.repository.BattleTagRepository;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.domain.tag.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final BattleService battleService;
    private final BattleTagRepository battleTagRepository;

    @Override
    public List<Tag> findByBattleId(UUID battleId) {
        Battle battle = battleService.findById(battleId);
        return battleTagRepository.findByBattle(battle).stream()
                .map(bt -> bt.getTag())
                .toList();
    }
}
