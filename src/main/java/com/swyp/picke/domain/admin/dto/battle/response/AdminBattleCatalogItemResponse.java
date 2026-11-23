package com.swyp.picke.domain.admin.dto.battle.response;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import java.time.LocalDateTime;

/** 콘텐츠 관리 화면 목록 행. */
public record AdminBattleCatalogItemResponse(
        Long battleId,
        String title,
        BattleDifficulty difficulty,
        BattleCategory category,
        boolean classRecommendable,
        BattleStatus status,
        LocalDateTime createdAt
) {
}
