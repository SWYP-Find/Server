package com.swyp.app.domain.perspective.controller;

import com.swyp.app.domain.perspective.dto.response.LikeCountResponse;
import com.swyp.app.domain.perspective.dto.response.LikeResponse;
import com.swyp.app.domain.perspective.service.PerspectiveLikeService;
import com.swyp.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "관점 좋아요 (Like)", description = "관점 좋아요 조회, 등록, 취소 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PerspectiveLikeController {

    private final PerspectiveLikeService likeService;

    @Operation(summary = "좋아요 수 조회", description = "특정 관점의 좋아요 수를 조회합니다.")
    @GetMapping("/perspectives/{perspectiveId}/likes")
    public ApiResponse<LikeCountResponse> getLikeCount(@PathVariable UUID perspectiveId) {
        return ApiResponse.onSuccess(likeService.getLikeCount(perspectiveId));
    }

    @Operation(summary = "좋아요 등록", description = "특정 관점에 좋아요를 등록합니다.")
    @PostMapping("/perspectives/{perspectiveId}/likes")
    public ApiResponse<LikeResponse> addLike(@PathVariable UUID perspectiveId) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        return ApiResponse.onSuccess(likeService.addLike(perspectiveId, userId));
    }

    @Operation(summary = "좋아요 취소", description = "특정 관점에 등록한 좋아요를 취소합니다.")
    @DeleteMapping("/perspectives/{perspectiveId}/likes")
    public ApiResponse<LikeResponse> removeLike(@PathVariable UUID perspectiveId) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        return ApiResponse.onSuccess(likeService.removeLike(perspectiveId, userId));
    }
}
