package com.swyp.app.domain.perspective.controller;

import com.swyp.app.domain.perspective.dto.response.LikeCountResponse;
import com.swyp.app.domain.perspective.dto.response.LikeResponse;
import com.swyp.app.domain.perspective.service.PerspectiveLikeService;
import com.swyp.app.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PerspectiveLikeController {

    private final PerspectiveLikeService likeService;

    @GetMapping("/perspectives/{perspectiveId}/likes")
    public ApiResponse<LikeCountResponse> getLikeCount(@PathVariable UUID perspectiveId) {
        return ApiResponse.onSuccess(likeService.getLikeCount(perspectiveId));
    }

    @PostMapping("/perspectives/{perspectiveId}/likes")
    public ApiResponse<LikeResponse> addLike(@PathVariable UUID perspectiveId) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        return ApiResponse.onSuccess(likeService.addLike(perspectiveId, userId));
    }

    @DeleteMapping("/perspectives/{perspectiveId}/likes")
    public ApiResponse<LikeResponse> removeLike(@PathVariable UUID perspectiveId) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        return ApiResponse.onSuccess(likeService.removeLike(perspectiveId, userId));
    }
}
