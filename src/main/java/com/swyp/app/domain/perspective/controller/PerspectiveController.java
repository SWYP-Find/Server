package com.swyp.app.domain.perspective.controller;

import com.swyp.app.domain.perspective.dto.request.CreatePerspectiveRequest;
import com.swyp.app.domain.perspective.dto.request.UpdatePerspectiveRequest;
import com.swyp.app.domain.perspective.dto.response.CreatePerspectiveResponse;
import com.swyp.app.domain.perspective.dto.response.MyPerspectiveResponse;
import com.swyp.app.domain.perspective.dto.response.PerspectiveListResponse;
import com.swyp.app.domain.perspective.dto.response.UpdatePerspectiveResponse;
import com.swyp.app.domain.perspective.service.PerspectiveService;
import com.swyp.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관점 (Perspective)", description = "관점 생성, 조회, 수정, 삭제 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PerspectiveController {

    private final PerspectiveService perspectiveService;

    @Operation(summary = "관점 생성", description = "특정 배틀에 대한 관점을 생성합니다. 사전 투표가 완료된 경우에만 가능합니다.")
    @PostMapping("/battles/{battleId}/perspectives")
    public ApiResponse<CreatePerspectiveResponse> createPerspective(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CreatePerspectiveRequest request
    ) {
        return ApiResponse.onSuccess(perspectiveService.createPerspective(battleId, userId, request));
    }

    @Operation(summary = "관점 리스트 조회", description = "특정 배틀의 관점 목록을 커서 기반 페이지네이션으로 조회합니다. optionLabel(A/B)로 필터링, sort(latest/popular)로 정렬 가능합니다.")
    @GetMapping("/battles/{battleId}/perspectives")
    public ApiResponse<PerspectiveListResponse> getPerspectives(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String optionLabel,
            @RequestParam(required = false, defaultValue = "latest") String sort
    ) {
        return ApiResponse.onSuccess(perspectiveService.getPerspectives(battleId, userId, cursor, size, optionLabel, sort));
    }

    @Operation(summary = "내 PENDING 관점 조회", description = "특정 배틀에서 내가 작성한 관점이 PENDING 상태인 경우 반환합니다. PENDING 관점이 없으면 404를 반환합니다.")
    @GetMapping("/battles/{battleId}/perspectives/me/pending")
    public ApiResponse<MyPerspectiveResponse> getMyPendingPerspective(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(perspectiveService.getMyPendingPerspective(battleId, userId));
    }

    @Operation(summary = "관점 삭제", description = "본인이 작성한 관점을 삭제합니다.")
    @DeleteMapping("/perspectives/{perspectiveId}")
    public ApiResponse<Void> deletePerspective(
            @PathVariable Long perspectiveId,
            @AuthenticationPrincipal Long userId) {
        perspectiveService.deletePerspective(perspectiveId, userId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "관점 검수 재시도", description = "검수 실패(MODERATION_FAILED) 상태의 관점에 대해 GPT 검수를 다시 요청합니다.")
    @PostMapping("/perspectives/{perspectiveId}/moderation/retry")
    public ApiResponse<Void> retryModeration(
            @PathVariable Long perspectiveId,
            @AuthenticationPrincipal Long userId) {
        perspectiveService.retryModeration(perspectiveId, userId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "관점 수정", description = "본인이 작성한 관점의 내용을 수정합니다.")
    @PatchMapping("/perspectives/{perspectiveId}")
    public ApiResponse<UpdatePerspectiveResponse> updatePerspective(
            @PathVariable Long perspectiveId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid UpdatePerspectiveRequest request
    ) {
        return ApiResponse.onSuccess(perspectiveService.updatePerspective(perspectiveId, userId, request));
    }
}
