package com.swyp.picke.domain.vote.controller;

import com.swyp.picke.domain.vote.dto.request.QuizVoteRequest;
import com.swyp.picke.domain.vote.dto.request.VoteRequest;
import com.swyp.picke.domain.vote.dto.response.*;
import com.swyp.picke.domain.vote.service.QuizVoteService;
import com.swyp.picke.domain.vote.service.VoteService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "투표 (Vote)", description = "사전/사후 투표 실행 및 통계, 내 투표 내역 조회 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoteController {

    // 배틀(BATTLE) 전용 서비스
    private final VoteService voteService;
    // 퀴즈(QUIZ) & 투표(POLL) 전용 서비스
    private final QuizVoteService quizVoteService;

    @Operation(summary = "[퀴즈] 선택 제출")
    @PostMapping("/battles/{battleId}/quiz-vote")
    public ApiResponse<QuizVoteResponse> submitQuiz(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestBody QuizVoteRequest request) {
        return ApiResponse.onSuccess(quizVoteService.submitQuiz(battleId, userId, request));
    }

    @Operation(summary = "[투표] 선택 제출")
    @PostMapping("/battles/{battleId}/poll-vote")
    public ApiResponse<PollVoteResponse> submitPoll(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestBody QuizVoteRequest request) {
        return ApiResponse.onSuccess(quizVoteService.submitPoll(battleId, userId, request));
    }

    @Operation(summary = "[퀴즈] 내 퀴즈 참여 내역 조회", description = "내가 선택한 퀴즈 옵션과 통계를 조회합니다.")
    @GetMapping("/battles/{battleId}/quiz-vote/me")
    public ApiResponse<QuizVoteResponse> getMyQuizVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(quizVoteService.getMyQuizVote(battleId, userId));
    }

    @Operation(summary = "[투표] 내 투표 참여 내역 조회", description = "내가 선택한 투표 옵션과 통계를 조회합니다.")
    @GetMapping("/battles/{battleId}/poll-vote/me")
    public ApiResponse<PollVoteResponse> getMyPollVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(quizVoteService.getMyPollVote(battleId, userId));
    }

    // 2. 배틀(BATTLE) 관련 API

    @Operation(summary = "[배틀] 사전 투표 실행", description = "배틀 진입 시 첫 투표(사전 투표)를 진행합니다.")
    @PostMapping("/battles/{battleId}/votes/pre")
    public ApiResponse<VoteResultResponse> preVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestBody VoteRequest request) {
        return ApiResponse.onSuccess(voteService.preVote(battleId, userId, request));
    }

    @Operation(summary = "[배틀] 사후 투표 실행", description = "콘텐츠 소비 후 최종 투표(사후 투표)를 진행합니다.")
    @PostMapping("/battles/{battleId}/votes/post")
    public ApiResponse<VoteResultResponse> postVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestBody VoteRequest request) {
        return ApiResponse.onSuccess(voteService.postVote(battleId, userId, request));
    }

    @Operation(summary = "[배틀] 투표 통계 조회", description = "특정 배틀의 옵션별 투표 수와 비율을 조회합니다.")
    @GetMapping("/battles/{battleId}/vote-stats")
    public ApiResponse<VoteStatsResponse> getVoteStats(@PathVariable Long battleId) {
        return ApiResponse.onSuccess(voteService.getVoteStats(battleId));
    }

    @Operation(summary = "[배틀] 내 투표 내역 조회", description = "특정 배틀에 대한 내 사전/사후 투표 내역과 현재 상태를 조회합니다.")
    @GetMapping("/battles/{battleId}/votes/me")
    public ApiResponse<MyVoteResponse> getMyVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(voteService.getMyVote(battleId, userId));
    }

    @Operation(summary = "[배틀] 오디오(TTS) 청취 완료 처리", description = "사전 투표 후, 오디오 재생이 완료되었을 때 호출하여 상태를 업데이트합니다.")
    @PostMapping("/battles/{battleId}/votes/tts-complete")
    public ApiResponse<Void> completeTts(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId) {
        voteService.completeTts(battleId, userId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "[관리자] 배틀 투표 삭제")
    @DeleteMapping("/admin/votes/battle/{voteId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteBattleVote(@PathVariable Long voteId) {
        voteService.deleteVote(voteId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "[관리자] 퀴즈/일반투표 기록 삭제")
    @DeleteMapping("/admin/votes/quiz-poll/{voteId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteQuizPollVote(@PathVariable Long voteId) {
        quizVoteService.deleteQuizVote(voteId);
        return ApiResponse.onSuccess(null);
    }
}
