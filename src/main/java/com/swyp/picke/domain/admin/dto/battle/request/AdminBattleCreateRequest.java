package com.swyp.picke.domain.admin.dto.battle.request;

import com.swyp.picke.domain.battle.enums.BattleStatus;
import java.util.List;

public record AdminBattleCreateRequest(
        String title,
        String summary,
        String description,
        String thumbnailUrl,
        BattleStatus status,
        List<Long> tagIds,
        List<AdminBattleOptionRequest> options
) {}

