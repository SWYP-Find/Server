package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.vote.dto.request.PollVoteRequest;
import com.swyp.picke.domain.vote.dto.response.PollVoteResponse;

public interface PollVoteService {
    PollVoteResponse submitPoll(Long battleId, Long userId, PollVoteRequest request);
    PollVoteResponse getMyPollVote(Long battleId, Long userId);
    void deletePollVoteByBattleId(Long battleId);
}
