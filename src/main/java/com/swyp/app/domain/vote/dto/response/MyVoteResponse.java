package com.swyp.app.domain.vote.dto.response;

import com.swyp.app.domain.vote.enums.VoteStatus;

public record MyVoteResponse(
        OptionInfo preVote,
        OptionInfo postVote,
        VoteStatus status
) {
    public record OptionInfo(Long optionId, String label, String title) {}
}
