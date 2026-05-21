package com.swyp.picke.domain.admin.dto.battle.request;

import java.util.List;

public record AdminBattleOptionRequest(
        String title,
        String stance,
        String representative,
        String imageUrl,
        Integer displayOrder,
        List<Long> tagIds
) {}
