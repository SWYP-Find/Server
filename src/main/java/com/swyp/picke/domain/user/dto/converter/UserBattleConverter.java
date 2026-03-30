package com.swyp.picke.domain.user.dto.converter;

import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.entity.UserBattle;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import org.springframework.stereotype.Component;

@Component
public class UserBattleConverter {

    /**
     * Entity -> DTO 변환
     */
    public UserBattleStatusResponse toStatusResponse(UserBattle userBattle) {
        return new UserBattleStatusResponse(
                userBattle.getBattle().getId(),
                userBattle.getStep()
        );
    }

    /**
     * 초기 데이터가 없을 때(NONE) 반환할 DTO 생성
     */
    public UserBattleStatusResponse toInitialResponse(Long battleId) {
        return new UserBattleStatusResponse(
                battleId,
                UserBattleStep.NONE
        );
    }
}
