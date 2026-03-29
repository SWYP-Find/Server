package com.swyp.app.domain.vote.service;

import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.vote.dto.request.VoteRequest;
import com.swyp.app.domain.vote.dto.response.MyVoteResponse;
import com.swyp.app.domain.vote.dto.response.VoteResultResponse;
import com.swyp.app.domain.vote.dto.response.VoteStatsResponse;

public interface VoteService {

    BattleOption findPreVoteOption(Long battleId, Long userId);

    Long findPostVoteOptionId(Long battleId, Long userId);

    VoteStatsResponse getVoteStats(Long battleId);

    MyVoteResponse getMyVote(Long battleId, Long userId);

    VoteResultResponse preVote(Long battleId, Long userId, VoteRequest request);

    VoteResultResponse postVote(Long battleId, Long userId, VoteRequest request);
}