package com.swyp.picke.domain.classroom.controller;

import com.swyp.picke.domain.classroom.dto.request.ClassDebateReplyRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassDebateResponse;
import com.swyp.picke.domain.classroom.service.debate.ClassDebateService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "클래스 AI 철학자 토론 API", description = "커스텀 배틀 클래스에서 사전투표 후 반대 입장의 AI 철학자와 3라운드 텍스트 토론 (PRD §8, §11)")
@RestController
@RequestMapping("/api/v1/classes/{classId}/battle/debate")
@RequiredArgsConstructor
public class ClassDebateController {

    private final ClassDebateService debateService;

    @Operation(summary = "토론 조회 (재접속)",
            description = "저장된 대화와 상태를 돌려줍니다(AI 호출 없음). 마감 후에도 읽을 수 있지만 canReply/canRetry 는 false 입니다. "
                    + "시작 전이면 404 CLASS_404_DEBATE.")
    @GetMapping
    public ApiResponse<ClassDebateResponse> get(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(debateService.get(userId, classId));
    }

    @Operation(summary = "토론 시작",
            description = "사전투표한 선택지의 반대편과 그 선택지의 철학자로 세션을 만들고 1라운드 AI 주장을 생성합니다. 시작하면 사전투표는 바꿀 수 없습니다. "
                    + "이미 시작했으면 저장된 대화를 그대로 돌려줍니다. 커스텀 클래스가 아니면 409 CLASS_409_DEBATE_TYPE, 사전투표 전이면 409 CLASS_409_DEBATE_PRE_VOTE. "
                    + "AI 생성에 실패하면 status=FAILED 로 응답하고 재시도 API 를 씁니다.")
    @PostMapping
    public ApiResponse<ClassDebateResponse> start(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(debateService.start(userId, classId));
    }

    @Operation(summary = "반박 제출",
            description = "현재 라운드의 반박(50~1,000자)을 저장하고 다음 라운드 AI 주장을 생성합니다. 3라운드 반박이면 토론이 끝나고(COMPLETED) 사후투표를 할 수 있습니다. "
                    + "같은 requestKey 재전송은 저장된 결과를 그대로 돌려주고, 다른 키로 같은 라운드를 다시 내면 409 CLASS_409_DEBATE_ROUND. "
                    + "반박을 받을 수 없는 상태면 409 CLASS_409_DEBATE_STATE, 길이 위반 400 CLASS_400_DEBATE_REPLY.")
    @PostMapping("/replies")
    public ApiResponse<ClassDebateResponse> reply(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId,
            @Valid @RequestBody ClassDebateReplyRequest request
    ) {
        return ApiResponse.onSuccess(debateService.reply(userId, classId, request));
    }

    @Operation(summary = "AI 주장 재시도",
            description = "status=FAILED(또는 3분 넘게 멈춘 생성)일 때 같은 라운드의 AI 주장을 같은 작업으로 다시 생성합니다. 그 외에는 409 CLASS_409_DEBATE_RETRY.")
    @PostMapping("/retry")
    public ApiResponse<ClassDebateResponse> retry(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long classId
    ) {
        return ApiResponse.onSuccess(debateService.retry(userId, classId));
    }
}
