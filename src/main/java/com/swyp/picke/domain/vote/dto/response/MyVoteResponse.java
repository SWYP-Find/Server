package com.swyp.picke.domain.vote.dto.response;

import com.swyp.picke.domain.vote.enums.VoteStatus;

public record MyVoteResponse(
        String battleTitle,
        OptionInfo preVote,
        OptionInfo postVote,
        VoteStatus status,
        boolean opinionChanged
) {
    public record OptionInfo(Long optionId, String label, String title) {}
}
