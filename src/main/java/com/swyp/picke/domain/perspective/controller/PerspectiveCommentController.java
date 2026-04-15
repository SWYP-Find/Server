package com.swyp.picke.domain.perspective.controller;

import com.swyp.picke.domain.perspective.dto.request.CreateCommentRequest;
import com.swyp.picke.domain.perspective.dto.request.UpdateCommentRequest;
import com.swyp.picke.domain.perspective.dto.response.CommentListResponse;
import com.swyp.picke.domain.perspective.dto.response.CreateCommentResponse;
import com.swyp.picke.domain.perspective.dto.response.UpdateCommentResponse;
import com.swyp.picke.domain.perspective.service.PerspectiveCommentService;
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

@Tag(name = "관점 댓글 API", description = "관점 댓글 생성, 조회, 수정, 삭제")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PerspectiveCommentController {

    private final PerspectiveCommentService commentService;

    @Operation(summary = "댓글 생성", description = "특정 관점에 댓글을 작성합니다.")
    @PostMapping("/perspectives/{perspectiveId}/comments")
    public ApiResponse<CreateCommentResponse> createComment(
            @PathVariable Long perspectiveId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CreateCommentRequest request
    ) {
        return ApiResponse.onSuccess(commentService.createComment(perspectiveId, userId, request));
    }

    @Operation(summary = "댓글 목록 조회", description = "특정 관점의 댓글 목록을 커서 기반 페이지네이션으로 조회합니다.")
    @GetMapping("/perspectives/{perspectiveId}/comments")
    public ApiResponse<CommentListResponse> getComments(
            @PathVariable Long perspectiveId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.onSuccess(commentService.getComments(perspectiveId, userId, cursor, size));
    }

    @Operation(summary = "댓글 목록 조회 (옵션 라벨)", description = "특정 관점의 댓글 목록을 커서 기반 페이지네이션으로 조회하며, stance를 투표한 옵션 라벨(A/B)로 반환합니다.")
    @GetMapping("/perspectives/{perspectiveId}/comments/labeled")
    public ApiResponse<CommentListResponse> getCommentsWithLabel(
            @PathVariable Long perspectiveId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.onSuccess(commentService.getCommentsWithLabel(perspectiveId, userId, cursor, size));
    }

    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글을 삭제합니다.")
    @DeleteMapping("/perspectives/{perspectiveId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long perspectiveId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId
    ) {
        commentService.deleteComment(perspectiveId, commentId, userId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글 내용을 수정합니다.")
    @PatchMapping("/perspectives/{perspectiveId}/comments/{commentId}")
    public ApiResponse<UpdateCommentResponse> updateComment(
            @PathVariable Long perspectiveId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid UpdateCommentRequest request
    ) {
        return ApiResponse.onSuccess(commentService.updateComment(perspectiveId, commentId, userId, request));
    }
}
