package com.swyp.app.domain.battle.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.entity.BattleOptionLabel;

import java.util.UUID;

public interface BattleService {

    Battle findById(UUID battleId);

    BattleOption findOptionById(UUID optionId);

    BattleOption findOptionByBattleIdAndLabel(UUID battleId, BattleOptionLabel label);
}
