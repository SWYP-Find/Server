package com.swyp.picke.domain.battle.dto.request;

import com.swyp.picke.domain.battle.enums.BattleStatus;
import java.time.LocalDate;
import java.util.List;

public record AdminBattleUpdateRequest(
        String title,
        String titlePrefix,
        String titleSuffix,
        String summary,
        String description,
        String thumbnailUrl,
        String itemA,
        String itemADesc,
        String itemB,
        String itemBDesc,
        LocalDate targetDate,
        Integer audioDuration,
        BattleStatus status,
        List<Long> tagIds,
        List<AdminBattleOptionRequest> options
) {}