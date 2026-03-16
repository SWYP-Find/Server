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
        UUID categoryId,
        LocalDate targetDate,
        List<UUID> tagIds,
        List<AdminBattleOptionRequest> options
) {}
