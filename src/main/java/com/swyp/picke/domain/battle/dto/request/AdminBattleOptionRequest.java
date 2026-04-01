package com.swyp.picke.domain.battle.dto.request;

import com.swyp.picke.domain.battle.enums.BattleOptionLabel;

import java.util.List;

public record AdminBattleOptionRequest(
        BattleOptionLabel label,
        String title,
        String stance,
        String representative,
        String quote,
        String imageUrl,
        Boolean isCorrect,
        List<Long> tagIds // 옵션 전용 태그 (철학자, 가치관 - 추후 사용자 유형 분석에 사용)
) {}