package com.swyp.app.domain.vote.service;

import com.swyp.app.domain.vote.dto.request.VoteRequest;
import com.swyp.app.domain.vote.dto.response.MyVoteResponse;
import com.swyp.app.domain.vote.dto.response.VoteResultResponse;
import com.swyp.app.domain.vote.dto.response.VoteStatsResponse;

import java.util.UUID;

public interface VoteService {

    UUID findPreVoteOptionId(UUID battleId, Long userId);

    VoteStatsResponse getVoteStats(UUID battleId);

    MyVoteResponse getMyVote(UUID battleId, Long userId);

    VoteResultResponse preVote(UUID battleId, Long userId, VoteRequest request);

    VoteResultResponse postVote(UUID battleId, Long userId, VoteRequest request);
}