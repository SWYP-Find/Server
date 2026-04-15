package com.swyp.picke.domain.vote.controller;

import com.swyp.picke.domain.vote.dto.request.PollVoteRequest;
import com.swyp.picke.domain.vote.dto.request.QuizVoteRequest;
import com.swyp.picke.domain.vote.dto.request.VoteRequest;
import com.swyp.picke.domain.vote.dto.response.MyVoteResponse;
import com.swyp.picke.domain.vote.dto.response.PollVoteResponse;
import com.swyp.picke.domain.vote.dto.response.QuizVoteResponse;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.picke.domain.vote.service.BattleVoteService;
import com.swyp.picke.domain.vote.service.PollVoteService;
import com.swyp.picke.domain.vote.service.QuizVoteService;
import com.swyp.picke.domain.vote.sse.SseEmitterRegistry;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Tag(name = "투표 API", description = "배틀/퀴즈/투표 투표 처리")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoteController {

    private final BattleVoteService battleVoteService;
    private final QuizVoteService quizVoteService;
    private final PollVoteService pollVoteService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Operation(summary = "[퀴즈] 답안 제출")
    @PostMapping("/battles/{battleId}/quiz-vote")
    public ApiResponse<QuizVoteResponse> submitQuiz(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestBody QuizVoteRequest request
    ) {
        return ApiResponse.onSuccess(quizVoteService.submitQuiz(battleId, userId, request));
    }

    @Operation(summary = "[투표] 선택 제출")
    @PostMapping("/battles/{battleId}/poll-vote")
    public ApiResponse<PollVoteResponse> submitPoll(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestBody PollVoteRequest request
    ) {
        return ApiResponse.onSuccess(pollVoteService.submitPoll(battleId, userId, request));
    }

    @Operation(summary = "[퀴즈] 내 퀴즈 참여 내역 조회", description = "내가 선택한 퀴즈 옵션과 통계를 조회합니다.")
    @GetMapping("/battles/{battleId}/quiz-vote/me")
    public ApiResponse<QuizVoteResponse> getMyQuizVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(quizVoteService.getMyQuizVote(battleId, userId));
    }

    @Operation(summary = "[투표] 내 투표 참여 내역 조회", description = "내가 선택한 투표 옵션과 통계를 조회합니다.")
    @GetMapping("/battles/{battleId}/poll-vote/me")
    public ApiResponse<PollVoteResponse> getMyPollVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(pollVoteService.getMyPollVote(battleId, userId));
    }

    @Operation(summary = "[배틀] 사전 투표 실행", description = "배틀 진입 시 첫 투표(사전 투표)를 진행합니다.")
    @PostMapping("/battles/{battleId}/votes/pre")
    public ApiResponse<VoteResultResponse> preVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestBody VoteRequest request
    ) {
        return ApiResponse.onSuccess(battleVoteService.preVote(battleId, userId, request));
    }

    @Operation(summary = "[배틀] 사후 투표 실행", description = "콘텐츠 소비 후 최종 투표(사후 투표)를 진행합니다.")
    @PostMapping("/battles/{battleId}/votes/post")
    public ApiResponse<VoteResultResponse> postVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestBody VoteRequest request
    ) {
        return ApiResponse.onSuccess(battleVoteService.postVote(battleId, userId, request));
    }

    @Operation(summary = "[배틀] 투표 통계 조회", description = "특정 배틀의 옵션별 투표 수와 비율을 조회합니다.")
    @GetMapping("/battles/{battleId}/vote-stats")
    public ApiResponse<VoteStatsResponse> getVoteStats(@PathVariable Long battleId) {
        return ApiResponse.onSuccess(battleVoteService.getVoteStats(battleId));
    }

    @Operation(summary = "[배틀] 투표 통계 실시간 구독", description = "post 투표 완료 후 투표 % 실시간 업데이트를 SSE로 수신합니다. 페이지 이탈 시 클라이언트에서 EventSource.close()를 호출해야 합니다.")
    @GetMapping(value = "/battles/{battleId}/vote-stats/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamVoteStats(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId
    ) {
        SseEmitter emitter = sseEmitterRegistry.register(battleId, userId);

        try {
            VoteStatsResponse stats = battleVoteService.getVoteStats(battleId);
            emitter.send(SseEmitter.event().name("vote-stats").data(stats));
        } catch (IOException e) {
            log.warn("SSE 초기 데이터 전송 실패 - battleId: {}, userId: {}", battleId, userId);
            sseEmitterRegistry.remove(battleId, userId);
        }

        return emitter;
    }

    @Operation(summary = "[배틀] 내 투표 내역 조회", description = "특정 배틀에 대한 내 사전/사후 투표 내역과 현재 상태를 조회합니다.")
    @GetMapping("/battles/{battleId}/votes/me")
    public ApiResponse<MyVoteResponse> getMyVote(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(battleVoteService.getMyVote(battleId, userId));
    }

    @Operation(summary = "[배틀] 오디오(TTS) 청취 완료 처리", description = "사전 투표 후, 오디오 재생이 완료되었을 때 호출하여 상태를 업데이트합니다.")
    @PostMapping("/battles/{battleId}/votes/tts-complete")
    public ApiResponse<Void> completeTts(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId
    ) {
        battleVoteService.completeTts(battleId, userId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "[관리자] 배틀 투표 기록 삭제")
    @DeleteMapping("/admin/votes/battle/{battleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteBattleVote(@PathVariable Long battleId) {
        battleVoteService.deleteVotesByBattleId(battleId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "[관리자] 퀴즈 투표 기록 삭제")
    @DeleteMapping("/admin/votes/quiz/{battleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteQuizVote(@PathVariable Long battleId) {
        quizVoteService.deleteQuizVoteByBattleId(battleId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "[관리자] 투표 콘텐츠 투표 기록 삭제")
    @DeleteMapping("/admin/votes/poll/{battleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deletePollVote(@PathVariable Long battleId) {
        pollVoteService.deletePollVoteByBattleId(battleId);
        return ApiResponse.onSuccess(null);
    }
}
