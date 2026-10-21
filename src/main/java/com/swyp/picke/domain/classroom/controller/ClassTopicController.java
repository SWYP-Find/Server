package com.swyp.picke.domain.classroom.controller;

import com.swyp.picke.domain.classroom.dto.request.ClassTopicDraftCreateRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassTopicSelectRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassTopicSuggestRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassTopicValidationRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassTopicDraftResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassTopicValidationResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassTopicVersionResponse;
import com.swyp.picke.domain.classroom.service.topic.ClassTopicAiService;
import com.swyp.picke.domain.classroom.service.topic.ClassTopicDraftService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "클래스 커스텀 주제 API", description = "커스텀 배틀 클래스를 만들기 위한 AI 주제 제안·선택·직접 수정 검증 (PRD §10)")
@RestController
@RequestMapping("/api/v1/class-topic-drafts")
@RequiredArgsConstructor
public class ClassTopicController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ClassTopicDraftService draftService;
    private final ClassTopicAiService aiService;

    @Operation(summary = "주제 초안 생성",
            description = "사용 중인 이용권이 있어야 합니다(403 CLASS_403_TICKET). 주제 2~200자, 카테고리 1~3개(400 CLASS_400_TOPIC_CONDITION). "
                    + "AI 를 호출하지 않으며, 초안마다 제안 3회·수정 검증 3회가 새로 주어집니다.")
    @PostMapping
    public ApiResponse<ClassTopicDraftResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ClassTopicDraftCreateRequest request
    ) {
        return ApiResponse.onSuccess(draftService.create(userId, request, LocalDateTime.now(KST)));
    }

    @Operation(summary = "주제 초안 조회",
            description = "저장된 후보·상태·남은 횟수를 그대로 돌려줍니다(AI 호출 없음). 제안 중 화면을 나갔다 돌아오면 이 API 로 결과를 이어 봅니다.")
    @GetMapping("/{draftId}")
    public ApiResponse<ClassTopicDraftResponse> get(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId
    ) {
        return ApiResponse.onSuccess(draftService.get(userId, draftId));
    }

    @Operation(summary = "AI 주제 제안 (최초·재추천)",
            description = "후보 8개를 만들어 초안에 저장하고 초안을 돌려줍니다. 본문으로 조건을 바꿀 수 있고 같은 초안의 새 배치로 셉니다. "
                    + "실패하면 suggestStatus=FAILED 로 응답하고 기존 후보와 횟수는 그대로입니다. "
                    + "맥락이 부족하면 NEEDS_CLARIFICATION + clarificationMessage. "
                    + "성공 배치 3회 소진 409 CLASS_409_TOPIC_SUGGEST_LIMIT, 생성 중 409 CLASS_409_TOPIC_GENERATING, 분당 2회 초과 429 CLASS_429_TOPIC.")
    @PostMapping("/{draftId}/suggestions")
    public ApiResponse<ClassTopicDraftResponse> suggest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId,
            @RequestBody(required = false) ClassTopicSuggestRequest request
    ) {
        return ApiResponse.onSuccess(aiService.suggest(userId, draftId, request));
    }

    @Operation(summary = "후보 선택",
            description = "후보를 검증된 주제 버전으로 저장합니다(AI 호출 없음). 화면의 batchNo 가 현재 배치와 다르면 409 CLASS_409_TOPIC_CANDIDATE. "
                    + "같은 후보를 다시 고르면 기존 버전을 돌려줍니다.")
    @PostMapping("/{draftId}/selection")
    public ApiResponse<ClassTopicVersionResponse> select(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId,
            @Valid @RequestBody ClassTopicSelectRequest request
    ) {
        return ApiResponse.onSuccess(draftService.select(userId, draftId, request));
    }

    @Operation(summary = "직접 수정 검증",
            description = "저장된 버전을 바탕으로 고친 질문·설명·선택지를 검증합니다. 길이·필수값 오류는 AI 호출 없이 issues 로 돌려주고 횟수를 쓰지 않습니다. "
                    + "통과하면 새 주제 버전을 만들고, 실패하면 issues 와 함께 마지막 검증 버전이 유지됩니다. AI 는 수정안을 고쳐 쓰지 않습니다. "
                    + "검증 3회 소진 409 CLASS_409_TOPIC_VALIDATION_LIMIT, AI 장애 503 CLASS_503_TOPIC_AI(횟수 차감 없음).")
    @PostMapping("/{draftId}/validations")
    public ApiResponse<ClassTopicValidationResponse> validate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long draftId,
            @Valid @RequestBody ClassTopicValidationRequest request
    ) {
        return ApiResponse.onSuccess(aiService.validate(userId, draftId, request));
    }
}
