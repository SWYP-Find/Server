package com.swyp.picke.domain.battle.dto.parse;

import jakarta.validation.constraints.NotBlank;

public record AdminBattleParseRequest(
        @NotBlank String rawText
) {}
