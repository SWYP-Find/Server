package com.swyp.picke.domain.user.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.user.dto.converter.UserBattleConverter;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserBattle;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.repository.UserBattleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * user_battles 는 (user_id, battle_id, class_id) 단위로 단계를 기록한다.
 * classroom == null 이면 일반 배틀 진행, 아니면 해당 클래스 안에서의 진행이며 서로 섞이지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBattleService {

    private final UserBattleRepository userBattleRepository;
    private final UserBattleConverter userBattleConverter;

    public UserBattleStatusResponse getUserBattleStatus(User user, Battle battle) {
        return getUserBattleStatus(user, battle, null);
    }

    public UserBattleStatusResponse getUserBattleStatus(User user, Battle battle, Classroom classroom) {
        return userBattleRepository.findByUserAndBattleAndClassroom(user, battle, classroom)
                .map(userBattleConverter::toStatusResponse)
                .orElseGet(() -> userBattleConverter.toInitialResponse(battle.getId()));
    }

    @Transactional
    public void upsertStep(User user, Battle battle, UserBattleStep newStep) {
        upsertStep(user, battle, null, newStep);
    }

    @Transactional
    public void upsertStep(User user, Battle battle, Classroom classroom, UserBattleStep newStep) {
        // 1. 먼저 찾거나, 없으면 새로 생성만 함 (DB 저장 X)
        UserBattle userBattle = userBattleRepository.findByUserAndBattleAndClassroom(user, battle, classroom)
                .orElseGet(() -> UserBattle.builder()
                        .user(user)
                        .battle(battle)
                        .classroom(classroom)
                        .step(UserBattleStep.NONE)
                        .build());

        // 2. 단계 업데이트 (신규 생성이든 기존 데이터든 여기서 무조건 객체가 존재함)
        userBattle.updateStep(newStep);

        // 3. 최종적으로 저장 (신규면 persist, 기존이면 merge/dirty checking)
        userBattleRepository.save(userBattle);
    }
}
