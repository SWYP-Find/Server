package com.swyp.picke.domain.battle.dto.response;

import java.time.LocalDateTime;

public record BattleSimpleResponse(
        Long battleId,
        String title,
        String type,
        String status,
        LocalDateTime createdAt
) {}