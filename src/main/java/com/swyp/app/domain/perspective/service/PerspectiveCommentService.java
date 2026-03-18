package com.swyp.app.domain.perspective.service;

import com.swyp.app.domain.perspective.dto.request.CreateCommentRequest;
import com.swyp.app.domain.perspective.dto.request.UpdateCommentRequest;
import com.swyp.app.domain.perspective.dto.response.CommentListResponse;
import com.swyp.app.domain.perspective.dto.response.CreateCommentResponse;
import com.swyp.app.domain.perspective.dto.response.UpdateCommentResponse;
import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveComment;
import com.swyp.app.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.app.domain.perspective.repository.PerspectiveRepository;
import com.swyp.app.domain.user.dto.response.UserSummary;
import com.swyp.app.domain.user.service.UserService;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerspectiveCommentService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveCommentRepository commentRepository;
    private final UserService userQueryService;

    @Transactional
    public CreateCommentResponse createComment(UUID perspectiveId, Long userId, CreateCommentRequest request) {
        Perspective perspective = findPerspectiveById(perspectiveId);

        PerspectiveComment comment = PerspectiveComment.builder()
                .perspective(perspective)
                .userId(userId)
                .content(request.content())
                .build();

        commentRepository.save(comment);
        perspective.incrementCommentCount();

        UserSummary user = userQueryService.findSummaryById(userId);
        return new CreateCommentResponse(
                comment.getId(),
                new CreateCommentResponse.UserSummary(user.userTag(), user.nickname(), user.characterType()),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    public CommentListResponse getComments(UUID perspectiveId, Long userId, String cursor, Integer size) {
        Perspective perspective = findPerspectiveById(perspectiveId);

        int pageSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : size;
        PageRequest pageable = PageRequest.of(0, pageSize);

        List<PerspectiveComment> comments = cursor == null
                ? commentRepository.findByPerspectiveOrderByCreatedAtDesc(perspective, pageable)
                : commentRepository.findByPerspectiveAndCreatedAtBeforeOrderByCreatedAtDesc(
                        perspective, LocalDateTime.parse(cursor), pageable);

        List<CommentListResponse.Item> items = comments.stream()
                .map(c -> {
                    UserSummary user = userQueryService.findSummaryById(c.getUserId());
                    return new CommentListResponse.Item(
                            c.getId(),
                            new CommentListResponse.UserSummary(user.userTag(), user.nickname(), user.characterType()),
                            c.getContent(),
                            c.getUserId().equals(userId),
                            c.getCreatedAt()
                    );
                })
                .toList();

        String nextCursor = comments.size() == pageSize
                ? comments.get(comments.size() - 1).getCreatedAt().toString()
                : null;

        return new CommentListResponse(items, nextCursor, nextCursor != null);
    }

    @Transactional
    public void deleteComment(UUID perspectiveId, UUID commentId, Long userId) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        PerspectiveComment comment = findCommentById(commentId);
        validateOwnership(comment, userId);

        commentRepository.delete(comment);
        perspective.decrementCommentCount();
    }

    @Transactional
    public UpdateCommentResponse updateComment(UUID perspectiveId, UUID commentId, Long userId, UpdateCommentRequest request) {
        findPerspectiveById(perspectiveId);
        PerspectiveComment comment = findCommentById(commentId);
        validateOwnership(comment, userId);

        comment.updateContent(request.content());
        return new UpdateCommentResponse(comment.getId(), comment.getContent(), comment.getUpdatedAt());
    }

    private Perspective findPerspectiveById(UUID perspectiveId) {
        return perspectiveRepository.findById(perspectiveId)
                .orElseThrow(() -> new CustomException(ErrorCode.PERSPECTIVE_NOT_FOUND));
    }

    private PerspectiveComment findCommentById(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateOwnership(PerspectiveComment comment, Long userId) {
        if (!comment.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMENT_FORBIDDEN);
        }
    }
}
