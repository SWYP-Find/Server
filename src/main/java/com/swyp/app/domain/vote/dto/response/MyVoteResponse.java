package com.swyp.app.domain.vote.dto.response;

import com.swyp.app.domain.vote.enums.VoteStatus;

import java.util.UUID;

public record MyVoteResponse(
        OptionInfo preVote,
        OptionInfo postVote,
        VoteStatus status
) {
    public record OptionInfo(UUID optionId, String label, String title) {}
}
