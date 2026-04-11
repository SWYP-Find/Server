package com.swyp.picke.domain.perspective.controller;

import com.swyp.picke.domain.perspective.dto.response.LikeResponse;
import com.swyp.picke.domain.perspective.service.CommentLikeService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "댓글 좋아요 (Comment Like)", description = "댓글 좋아요 등록, 취소 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    @Operation(summary = "댓글 좋아요 등록", description = "특정 댓글에 좋아요를 등록합니다.")
    @PostMapping("/comments/{commentId}/likes")
    public ApiResponse<LikeResponse> addLike(@PathVariable Long commentId,
                                              @AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(commentLikeService.addLike(commentId, userId));
    }

    @Operation(summary = "댓글 좋아요 취소", description = "특정 댓글에 등록한 좋아요를 취소합니다.")
    @DeleteMapping("/comments/{commentId}/likes")
    public ApiResponse<LikeResponse> removeLike(@PathVariable Long commentId,
                                                 @AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(commentLikeService.removeLike(commentId, userId));
    }
}
