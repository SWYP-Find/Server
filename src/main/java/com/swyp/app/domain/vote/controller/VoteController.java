package com.swyp.app.domain.vote.controller;

import com.swyp.app.domain.vote.dto.response.MyVoteResponse;
import com.swyp.app.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.app.domain.vote.service.VoteService;
import com.swyp.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "투표 (Vote)", description = "투표 통계 및 내 투표 내역 조회 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @Operation(summary = "투표 통계 조회", description = "특정 배틀의 옵션별 투표 수와 비율을 조회합니다.")
    @GetMapping("/battles/{battleId}/vote-stats")
    public ApiResponse<VoteStatsResponse> getVoteStats(@PathVariable UUID battleId) {
        return ApiResponse.onSuccess(voteService.getVoteStats(battleId));
    }

    @Operation(summary = "내 투표 내역 조회", description = "특정 배틀에 대한 내 사전/사후 투표 내역과 생각 변화 여부를 조회합니다.")
    @GetMapping("/battles/{battleId}/votes/me")
    public ApiResponse<MyVoteResponse> getMyVote(@PathVariable UUID battleId) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        return ApiResponse.onSuccess(voteService.getMyVote(battleId, userId));
    }
}
