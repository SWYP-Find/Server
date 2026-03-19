package com.swyp.app.domain.battle.dto.request;

import com.swyp.app.domain.battle.enums.BattleType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminBattleCreateRequest(
        String title,
        String summary,
        String description,
        String thumbnailUrl,
        BattleType type,
        LocalDate targetDate,
        List<UUID> tagIds, // 배틀 공통 태그 (카테고리, 가치관, 철학자)
        List<AdminBattleOptionRequest> options
) {}