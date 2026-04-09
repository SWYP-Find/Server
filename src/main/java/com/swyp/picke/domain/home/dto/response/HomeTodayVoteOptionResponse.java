package com.swyp.picke.domain.home.dto.response;

import com.swyp.picke.domain.battle.enums.BattleOptionLabel;

public record HomeTodayVoteOptionResponse(
        BattleOptionLabel label,
        String title
) {}
