package com.swyp.picke.domain.admin.dto.battle.request;

import com.swyp.picke.domain.battle.enums.BattleStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminBattleCreateRequest(
        String title,
        String summary,
        String description,
        String thumbnailUrl,
        LocalDate targetDate,
        LocalDateTime publishAt,
        Integer audioDuration,
        BattleStatus status,
        List<Long> tagIds,
        List<AdminBattleOptionRequest> options,
        int botCount
) {}

