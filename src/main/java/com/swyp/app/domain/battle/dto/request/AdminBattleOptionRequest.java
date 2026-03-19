package com.swyp.app.domain.battle.dto.request;

import com.swyp.app.domain.battle.enums.BattleOptionLabel;

import java.util.List;
import java.util.UUID;

public record AdminBattleOptionRequest(
        BattleOptionLabel label,
        String title,
        String stance,
        String representative,
        String quote,
        String imageUrl,
        List<UUID> tagIds // 옵션 전용 태그 (철학자, 가치관 - 추후 사용자 유형 분석에 사용)
) {}