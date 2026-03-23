package com.swyp.app.domain.battle.dto.request;

import com.swyp.app.domain.battle.enums.BattleStatus;

import java.time.LocalDate;
import java.util.List;

public record AdminBattleUpdateRequest(
        String title,
        String summary,
        String description,
        String thumbnailUrl,
        LocalDate targetDate,
        Integer audioDuration,
        BattleStatus status,
        List<Long> tagIds // 배틀 공통 태그 수정용 (카테고리, 가치관, 철학자)
) {}