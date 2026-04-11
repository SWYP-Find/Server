package com.swyp.picke.domain.perspective.controller;

import com.swyp.picke.domain.perspective.dto.request.CreatePerspectiveRequest;
import com.swyp.picke.domain.perspective.dto.request.UpdatePerspectiveRequest;
import com.swyp.picke.domain.perspective.dto.response.CreatePerspectiveResponse;
import com.swyp.picke.domain.perspective.dto.response.MyPerspectiveResponse;
import com.swyp.picke.domain.perspective.dto.response.PerspectiveDetailResponse;
import com.swyp.picke.domain.perspective.dto.response.PerspectiveListResponse;
import com.swyp.picke.domain.perspective.dto.response.UpdatePerspectiveResponse;
import com.swyp.picke.domain.perspective.service.PerspectiveService;
import com.swyp.picke.global.common.response.ApiResponse;
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

@Tag(name = "관점 API", description = "관점 생성, 조회, 수정, 삭제")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PerspectiveController {

    private final PerspectiveService perspectiveService;

    @Operation(summary = "관점 상세 조회", description = "특정 관점의 상세 정보를 조회합니다.")
    @GetMapping("/perspectives/{perspectiveId}")
    public ApiResponse<PerspectiveDetailResponse> getPerspectiveDetail(
            @PathVariable Long perspectiveId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(perspectiveService.getPerspectiveDetail(perspectiveId, userId));
    }

    @Operation(summary = "관점 생성", description = "특정 배틀에 대한 사용자 관점을 생성합니다.")
    @PostMapping("/battles/{battleId}/perspectives")
    public ApiResponse<CreatePerspectiveResponse> createPerspective(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CreatePerspectiveRequest request
    ) {
        return ApiResponse.onSuccess(perspectiveService.createPerspective(battleId, userId, request));
    }

    @Operation(summary = "관점 목록 조회", description = "특정 배틀의 관점 목록을 커서 기반으로 조회합니다.")
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

    @Operation(summary = "내 관점 조회", description = "해당 배틀에서 본인이 작성한 관점을 조회합니다.")
    @GetMapping("/battles/{battleId}/perspectives/me")
    public ApiResponse<MyPerspectiveResponse> getMyPerspective(
            @PathVariable Long battleId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(perspectiveService.getMyPerspective(battleId, userId));
    }

    @Operation(summary = "관점 삭제", description = "본인이 작성한 관점을 삭제합니다.")
    @DeleteMapping("/perspectives/{perspectiveId}")
    public ApiResponse<Void> deletePerspective(
            @PathVariable Long perspectiveId,
            @AuthenticationPrincipal Long userId) {
        perspectiveService.deletePerspective(perspectiveId, userId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "관점 검수 재요청", description = "검수 실패 상태의 관점에 대해 검수를 다시 요청합니다.")
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
