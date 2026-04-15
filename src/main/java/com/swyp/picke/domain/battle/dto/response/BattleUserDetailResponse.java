package com.swyp.picke.domain.battle.dto.response;

import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.enums.VoteSide;

import java.util.List;

public record BattleUserDetailResponse(
        BattleSummaryResponse battleInfo,
        String description,
        String shareUrl,
        VoteSide userVoteStatus,
        UserBattleStep currentStep,
        List<BattleTagResponse> categoryTags,
        List<BattleTagResponse> philosopherTags,
        List<BattleTagResponse> valueTags
) {}
