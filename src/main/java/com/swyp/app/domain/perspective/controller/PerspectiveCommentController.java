package com.swyp.app.domain.perspective.controller;

import com.swyp.app.domain.perspective.dto.request.CreateCommentRequest;
import com.swyp.app.domain.perspective.dto.request.UpdateCommentRequest;
import com.swyp.app.domain.perspective.dto.response.CommentListResponse;
import com.swyp.app.domain.perspective.dto.response.CreateCommentResponse;
import com.swyp.app.domain.perspective.dto.response.UpdateCommentResponse;
import com.swyp.app.domain.perspective.service.PerspectiveCommentService;
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
public class PerspectiveCommentController {

    private final PerspectiveCommentService commentService;

    @PostMapping("/perspectives/{perspectiveId}/comments")
    public ApiResponse<CreateCommentResponse> createComment(
            @PathVariable UUID perspectiveId,
            @RequestBody @Valid CreateCommentRequest request
    ) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        return ApiResponse.onSuccess(commentService.createComment(perspectiveId, userId, request));
    }

    @GetMapping("/perspectives/{perspectiveId}/comments")
    public ApiResponse<CommentListResponse> getComments(
            @PathVariable UUID perspectiveId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        return ApiResponse.onSuccess(commentService.getComments(perspectiveId, userId, cursor, size));
    }

    @DeleteMapping("/perspectives/{perspectiveId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable UUID perspectiveId,
            @PathVariable UUID commentId
    ) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        commentService.deleteComment(perspectiveId, commentId, userId);
        return ApiResponse.onSuccess(null);
    }

    @PatchMapping("/perspectives/{perspectiveId}/comments/{commentId}")
    public ApiResponse<UpdateCommentResponse> updateComment(
            @PathVariable UUID perspectiveId,
            @PathVariable UUID commentId,
            @RequestBody @Valid UpdateCommentRequest request
    ) {
        // TODO: Security 적용 후 @AuthenticationPrincipal로 userId 교체
        Long userId = 1L;
        return ApiResponse.onSuccess(commentService.updateComment(perspectiveId, commentId, userId, request));
    }
}
