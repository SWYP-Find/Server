package com.swyp.picke.domain.user.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.user.dto.converter.UserBattleConverter;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserBattle;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.repository.UserBattleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBattleService {

    private final UserBattleRepository userBattleRepository;
    private final UserBattleConverter userBattleConverter;

    public UserBattleStatusResponse getUserBattleStatus(User user, Battle battle) {
        return userBattleRepository.findByUserAndBattle(user, battle)
                .map(userBattleConverter::toStatusResponse)
                .orElseGet(() -> userBattleConverter.toInitialResponse(battle.getId()));
    }

    @Transactional
    public void upsertStep(User user, Battle battle, UserBattleStep newStep) {
        // 1. 먼저 찾거나, 없으면 새로 생성만 함 (DB 저장 X)
        UserBattle userBattle = userBattleRepository.findByUserAndBattle(user, battle)
                .orElseGet(() -> UserBattle.builder()
                        .user(user)
                        .battle(battle)
                        .step(UserBattleStep.NONE)
                        .build());

        // 2. 단계 업데이트 (신규 생성이든 기존 데이터든 여기서 무조건 객체가 존재함)
        userBattle.updateStep(newStep);

        // 3. 최종적으로 저장 (신규면 persist, 기존이면 merge/dirty checking)
        userBattleRepository.save(userBattle);
    }
}