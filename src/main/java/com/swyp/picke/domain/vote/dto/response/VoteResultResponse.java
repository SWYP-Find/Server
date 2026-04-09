package com.swyp.picke.domain.vote.dto.response;

import com.swyp.picke.domain.user.enums.UserBattleStep;

public record VoteResultResponse(
        Long voteId,
        UserBattleStep status
) {}