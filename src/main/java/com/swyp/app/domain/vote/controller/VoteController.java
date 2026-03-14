package com.swyp.app.domain.vote.controller;

import com.swyp.app.domain.vote.dto.response.MyVoteResponse;
import com.swyp.app.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.app.domain.vote.service.VoteService;
import com.swyp.app.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @GetMapping("/battles/{battleId}/vote-stats")
    public ApiResponse<VoteStatsResponse> getVoteStats(@PathVariable UUID battleId) {
        return ApiResponse.onSuccess(voteService.getVoteStats(battleId));
    }

    @GetMapping("/battles/{battleId}/votes/me")
    public ApiResponse<MyVoteResponse> getMyVote(@PathVariable UUID battleId) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        return ApiResponse.onSuccess(voteService.getMyVote(battleId, userId));
    }
}
