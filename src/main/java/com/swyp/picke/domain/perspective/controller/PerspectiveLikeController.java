package com.swyp.picke.domain.perspective.controller;

import com.swyp.picke.domain.perspective.dto.response.LikeCountResponse;
import com.swyp.picke.domain.perspective.dto.response.LikeResponse;
import com.swyp.picke.domain.perspective.service.PerspectiveLikeService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관점 좋아요 API", description = "관점 좋아요 조회, 등록, 취소")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PerspectiveLikeController {

    private final PerspectiveLikeService likeService;

    @Operation(summary = "좋아요 수 조회", description = "특정 관점의 좋아요 수를 조회합니다.")
    @GetMapping("/perspectives/{perspectiveId}/likes")
    public ApiResponse<LikeCountResponse> getLikeCount(@PathVariable Long perspectiveId) {
        return ApiResponse.onSuccess(likeService.getLikeCount(perspectiveId));
    }

    @Operation(summary = "좋아요 등록", description = "특정 관점에 좋아요를 등록합니다.")
    @PostMapping("/perspectives/{perspectiveId}/likes")
    public ApiResponse<LikeResponse> addLike(
            @PathVariable Long perspectiveId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(likeService.addLike(perspectiveId, userId));
    }

    @Operation(summary = "좋아요 취소", description = "특정 관점의 좋아요를 취소합니다.")
    @DeleteMapping("/perspectives/{perspectiveId}/likes")
    public ApiResponse<LikeResponse> removeLike(
            @PathVariable Long perspectiveId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(likeService.removeLike(perspectiveId, userId));
    }
}
