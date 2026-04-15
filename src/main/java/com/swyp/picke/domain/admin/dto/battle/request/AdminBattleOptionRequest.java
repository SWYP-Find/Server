package com.swyp.picke.domain.admin.dto.battle.request;

import com.swyp.picke.domain.battle.enums.BattleOptionLabel;

import java.util.List;

public record AdminBattleOptionRequest(
        BattleOptionLabel label,
        String title,
        String stance,
        String representative,
        String imageUrl,
        List<Long> tagIds
) {}
