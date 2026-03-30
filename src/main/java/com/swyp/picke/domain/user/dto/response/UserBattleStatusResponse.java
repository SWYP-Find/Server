package com.swyp.picke.domain.user.dto.response;

import com.swyp.picke.domain.user.enums.UserBattleStep;

/**
 * 사용자의 배틀 진행 단계를 전달하는 순수 데이터 객체
 */
public record UserBattleStatusResponse(
        Long battleId,
        UserBattleStep step
) {
}