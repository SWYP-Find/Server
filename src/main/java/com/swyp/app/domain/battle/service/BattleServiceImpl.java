package com.swyp.app.domain.battle.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.entity.BattleOptionLabel;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BattleServiceImpl implements BattleService {

    @Override
    public Battle findById(UUID battleId) {
        throw new UnsupportedOperationException("Not yet implemented - pending Battle domain merge");
    }

    @Override
    public BattleOption findOptionById(UUID optionId) {
        throw new UnsupportedOperationException("Not yet implemented - pending Battle domain merge");
    }

    @Override
    public BattleOption findOptionByBattleIdAndLabel(UUID battleId, BattleOptionLabel label) {
        throw new UnsupportedOperationException("Not yet implemented - pending Battle domain merge");
    }
}
