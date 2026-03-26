package com.swyp.app.domain.battle.dto.request;

import com.swyp.app.domain.battle.enums.BattleType;
import java.time.LocalDate;
import java.util.List;

public record AdminBattleCreateRequest(
        String title,
        String titlePrefix,
        String titleSuffix,
        String summary,
        String description,
        String thumbnailUrl,
        BattleType type,
        String itemA,
        String itemADesc,
        String itemB,
        String itemBDesc,
        LocalDate targetDate,
        List<Long> tagIds,
        List<AdminBattleOptionRequest> options
) {}