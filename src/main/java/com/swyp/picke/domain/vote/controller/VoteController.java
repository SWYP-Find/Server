package com.swyp.picke.domain.vote.controller;

import com.swyp.picke.domain.vote.dto.request.VoteRequest;
import com.swyp.picke.domain.vote.dto.response.MyVoteResponse;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.picke.domain.vote.service.VoteService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "투표 (Vote)", description = "사전/사후 투표 실행 및 통계, 내 투표 내역 조회 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @Operation(summary = "사전 투표 실행", description = "배틀 진입 시 첫 투표(사전 투표)를 진행합니다.")
    @PostMapping("/battles/{battleId}/votes/pre")
    public ApiResponse<VoteResultResponse> preVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestBody VoteRequest request) {
        return ApiResponse.onSuccess(voteService.preVote(battleId, userId, request));
    }

    @Operation(summary = "사후 투표 실행", description = "콘텐츠 소비 후 최종 투표(사후 투표)를 진행합니다.")
    @PostMapping("/battles/{battleId}/votes/post")
    public ApiResponse<VoteResultResponse> postVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestBody VoteRequest request) {
        return ApiResponse.onSuccess(voteService.postVote(battleId, userId, request));
    }

    @Operation(summary = "투표 통계 조회", description = "특정 배틀의 옵션별 투표 수와 비율을 조회합니다.")
    @GetMapping("/battles/{battleId}/vote-stats")
    public ApiResponse<VoteStatsResponse> getVoteStats(@PathVariable Long battleId) {
        return ApiResponse.onSuccess(voteService.getVoteStats(battleId));
    }

    @Operation(summary = "내 투표 내역 조회", description = "특정 배틀에 대한 내 사전/사후 투표 내역과 현재 상태를 조회합니다.")
    @GetMapping("/battles/{battleId}/votes/me")
    public ApiResponse<MyVoteResponse> getMyVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(voteService.getMyVote(battleId, userId));
    }

    @Operation(summary = "오디오(TTS) 청취 완료 처리", description = "사전 투표 후, 오디오 재생이 완료되었을 때 호출하여 상태를 업데이트합니다.")
    @PostMapping("/battles/{battleId}/votes/tts-complete")
    public ApiResponse<Void> completeTts(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId) {
        voteService.completeTts(battleId, userId);
        return ApiResponse.onSuccess(null);
    }
}
