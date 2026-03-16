package com.swyp.app.domain.battle.dto.request;

import com.swyp.app.domain.battle.enums.BattleStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminBattleUpdateRequest(
        String title,
        String summary,
        String description,
        String thumbnailUrl,
        LocalDate targetDate,
        Integer audioDuration,
        BattleStatus status,
        List<UUID> tagIds
) {}