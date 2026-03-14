package com.swyp.app.domain.perspective.controller;

import com.swyp.app.domain.perspective.dto.request.CreatePerspectiveRequest;
import com.swyp.app.domain.perspective.dto.request.UpdatePerspectiveRequest;
import com.swyp.app.domain.perspective.dto.response.CreatePerspectiveResponse;
import com.swyp.app.domain.perspective.dto.response.PerspectiveListResponse;
import com.swyp.app.domain.perspective.dto.response.UpdatePerspectiveResponse;
import com.swyp.app.domain.perspective.service.PerspectiveService;
import com.swyp.app.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PerspectiveController {

    private final PerspectiveService perspectiveService;

    @PostMapping("/battles/{battleId}/perspectives")
    public ApiResponse<CreatePerspectiveResponse> createPerspective(
            @PathVariable UUID battleId,
            @RequestBody @Valid CreatePerspectiveRequest request
    ) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        return ApiResponse.onSuccess(perspectiveService.createPerspective(battleId, userId, request));
    }

    @GetMapping("/battles/{battleId}/perspectives")
    public ApiResponse<PerspectiveListResponse> getPerspectives(
            @PathVariable UUID battleId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String optionLabel
    ) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        return ApiResponse.onSuccess(perspectiveService.getPerspectives(battleId, userId, cursor, size, optionLabel));
    }

    @DeleteMapping("/perspectives/{perspectiveId}")
    public ApiResponse<Void> deletePerspective(@PathVariable UUID perspectiveId) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        perspectiveService.deletePerspective(perspectiveId, userId);
        return ApiResponse.onSuccess(null);
    }

    @PatchMapping("/perspectives/{perspectiveId}")
    public ApiResponse<UpdatePerspectiveResponse> updatePerspective(
            @PathVariable UUID perspectiveId,
            @RequestBody @Valid UpdatePerspectiveRequest request
    ) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        return ApiResponse.onSuccess(perspectiveService.updatePerspective(perspectiveId, userId, request));
    }
}
