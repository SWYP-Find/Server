package com.swyp.app.domain.battle.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.entity.BattleOptionLabel;
import com.swyp.app.domain.battle.repository.BattleOptionRepository;
import com.swyp.app.domain.battle.repository.BattleRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BattleServiceImpl implements BattleService {

    private final BattleRepository battleRepository;
    private final BattleOptionRepository battleOptionRepository;

    @Override
    public Battle findById(UUID battleId) {
        return battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));
    }

    @Override
    public BattleOption findOptionById(UUID optionId) {
        return battleOptionRepository.findById(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));
    }

    @Override
    public BattleOption findOptionByBattleIdAndLabel(UUID battleId, BattleOptionLabel label) {
        Battle battle = findById(battleId);
        return battleOptionRepository.findByBattleAndLabel(battle, label)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));
    }
}
