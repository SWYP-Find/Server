package com.swyp.picke.domain.classroom.controller;

import com.swyp.picke.domain.classroom.dto.response.ClassBattleDetailResponse;
import com.swyp.picke.domain.classroom.service.ClassBattleService;
import com.swyp.picke.domain.perspective.dto.request.CreatePerspectiveRequest;
import com.swyp.picke.domain.perspective.dto.response.CreatePerspectiveResponse;
import com.swyp.picke.domain.perspective.dto.response.MyPerspectiveResponse;
import com.swyp.picke.domain.perspective.dto.response.PerspectiveListResponse;
import com.swyp.picke.domain.vote.dto.request.VoteRequest;
import com.swyp.picke.domain.vote.dto.response.MyVoteResponse;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 클래스 안에서의 배틀 진행. 배틀 ID 는 경로에 없고 클래스에 붙은 배틀을 쓴다.
 * 공통 에러: 없는/삭제된 클래스 404 CLASS_404, 멤버 아님 403 CLASS_403_MEMBER.
 * 쓰기 공통: 참여하지 않는 운영자 403 CLASS_403_MEMBER, 이용권 종료 409 CLASS_409_TICKET_ENDED, 마감일 경과 409 CLASS_409_CLOSED.
 */
@Tag(name = "클래스 배틀 API", description = "클래스 안에서 배틀 상세·투표·의견 진행 (일반 배틀과 별도 집계)")
@RestController
@RequestMapping("/api/v1/classes/{classId}/battle")
@RequiredArgsConstructor
public class ClassBattleController {

    private final ClassBattleService classBattleService;

    @Operation(summary = "클래스 배틀 상세",
            description = "배틀 상세 + 이 클래스 안에서의 내 투표 상태·진행 단계·참여자 수. 마감 후에도 조회 가능. "
                    + "canParticipate 가 false 면 투표·의견 버튼을 막고, participationCompleted 로 완료 표시를 결정합니다.")
    @GetMapping
    public ApiResponse<ClassBattleDetailResponse> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(classBattleService.getDetail(userId, classId));
    }

    @Operation(summary = "클래스 사전투표",
            description = "배틀 시작 전(NONE/PRE_VOTE)에는 변경 가능, TTS 청취 이후에는 409 CLASS_409_PRE_VOTE_LOCKED. "
                    + "크레딧 차감·성향 점수·일반 참여자 수에는 영향 없음.")
    @PostMapping("/votes/pre")
    public ApiResponse<VoteResultResponse> preVote(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId,
            @RequestBody VoteRequest request
    ) {
        return ApiResponse.onSuccess(classBattleService.preVote(userId, classId, request));
    }

    @Operation(summary = "클래스 TTS 청취 완료", description = "사전투표 후 오디오 재생이 끝났을 때 호출. 이미 완료(COMPLETED)된 진행은 되돌리지 않습니다.")
    @PostMapping("/votes/tts-complete")
    public ApiResponse<Void> completeTts(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        classBattleService.completeTts(userId, classId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "클래스 중간투표",
            description = "중간투표가 있는 기존 콘텐츠(상세의 midVoteEnabled=true)에서 콘텐츠 중 선택 화면에서 고른 선택지를 기록합니다. "
                    + "optionId 는 배틀 선택지 ID(A/B). 사후투표 전까지 다시 보낼 수 있고, 보내면 사전투표는 잠깁니다. "
                    + "중간투표가 없는 배틀이면 409 CLASS_409_MID_VOTE, 사전투표 전이면 404 VOTE_NOT_FOUND, 사후투표 후면 409 CLASS_409_MID_VOTE_LOCKED.")
    @PostMapping("/votes/mid")
    public ApiResponse<VoteResultResponse> midVote(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId,
            @RequestBody VoteRequest request
    ) {
        return ApiResponse.onSuccess(classBattleService.midVote(userId, classId, request));
    }

    @Operation(summary = "클래스 사후투표",
            description = "사전투표 없이 호출하면 400 PRE_VOTE_REQUIRED, 이미 제출했으면 409 VOTE_ALREADY_SUBMITTED. 제출 후에는 변경 불가.")
    @PostMapping("/votes/post")
    public ApiResponse<VoteResultResponse> postVote(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId,
            @RequestBody VoteRequest request
    ) {
        return ApiResponse.onSuccess(classBattleService.postVote(userId, classId, request));
    }

    @Operation(summary = "클래스 내 투표 내역", description = "이 클래스 안에서의 내 사전/중간/사후 투표와 진행 단계. 투표 전이면 404 VOTE_NOT_FOUND.")
    @GetMapping("/votes/me")
    public ApiResponse<MyVoteResponse> getMyVote(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(classBattleService.getMyVote(userId, classId));
    }

    @Operation(summary = "클래스 투표 통계", description = "이 클래스 멤버들의 사후투표만 집계한 옵션별 수·비율.")
    @GetMapping("/vote-stats")
    public ApiResponse<VoteStatsResponse> getVoteStats(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(classBattleService.getVoteStats(userId, classId));
    }

    @Operation(summary = "클래스 의견 등록", description = "클래스 안에서 사후투표한 선택지에 귀속되는 의견을 등록합니다. 사후투표 전이면 404 BATTLE_OPTION_NOT_FOUND.")
    @PostMapping("/perspectives")
    public ApiResponse<CreatePerspectiveResponse> createPerspective(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId,
            @RequestBody @Valid CreatePerspectiveRequest request
    ) {
        return ApiResponse.onSuccess(classBattleService.createPerspective(userId, classId, request));
    }

    @Operation(summary = "클래스 의견 목록", description = "이 클래스 안에서 작성된 의견만 커서 기반으로 조회합니다. sort=latest|popular, optionId 로 선택지 필터.")
    @GetMapping("/perspectives")
    public ApiResponse<PerspectiveListResponse> getPerspectives(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long optionId,
            @RequestParam(required = false, defaultValue = "latest") String sort
    ) {
        return ApiResponse.onSuccess(classBattleService.getPerspectives(userId, classId, cursor, size, optionId, sort));
    }

    @Operation(summary = "클래스 내 의견", description = "이 클래스 안에서 내가 작성한 의견 목록.")
    @GetMapping("/perspectives/me")
    public ApiResponse<List<MyPerspectiveResponse>> getMyPerspectives(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(classBattleService.getMyPerspectives(userId, classId));
    }
}
