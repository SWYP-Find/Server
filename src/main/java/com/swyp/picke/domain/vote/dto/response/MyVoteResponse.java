package com.swyp.picke.domain.vote.dto.response;

import com.swyp.picke.domain.user.enums.UserBattleStep;

public record MyVoteResponse(
        String battleTitle,
        OptionInfo preVote,
        OptionInfo postVote,
        UserBattleStep status,
        boolean opinionChanged
) {
    public record OptionInfo(Long optionId, String label, String title) {}
}
