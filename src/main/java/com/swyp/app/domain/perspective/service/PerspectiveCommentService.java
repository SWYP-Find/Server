package com.swyp.app.domain.perspective.service;

import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.domain.perspective.dto.request.CreateCommentRequest;
import com.swyp.app.domain.perspective.dto.request.UpdateCommentRequest;
import com.swyp.app.domain.perspective.dto.response.CommentListResponse;
import com.swyp.app.domain.perspective.dto.response.CreateCommentResponse;
import com.swyp.app.domain.perspective.dto.response.UpdateCommentResponse;
import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveComment;
import com.swyp.app.domain.perspective.repository.CommentLikeRepository;
import com.swyp.app.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.app.domain.perspective.repository.PerspectiveRepository;
import com.swyp.app.domain.user.dto.response.UserSummary;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.domain.user.service.UserService;
import com.swyp.app.domain.vote.service.VoteService;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerspectiveCommentService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserService userQueryService;
    private final VoteService voteService;
    private final BattleService battleService;

    @Transactional
    public CreateCommentResponse createComment(Long perspectiveId, Long userId, CreateCommentRequest request) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        PerspectiveComment comment = PerspectiveComment.builder()
                .perspective(perspective)
                .user(user)
                .content(request.content())
                .build();

        commentRepository.save(comment);
        perspective.incrementCommentCount();

        UserSummary userSummary = userQueryService.findSummaryById(userId);
        Long postVoteOptionId = voteService.findPostVoteOptionId(perspective.getBattle().getId(), userId);
        String stance = null;
        if (postVoteOptionId != null) {
            stance = battleService.findOptionById(postVoteOptionId).getStance();
        }
        return new CreateCommentResponse(
                comment.getId(),
                new CreateCommentResponse.UserSummary(userSummary.userTag(), userSummary.nickname(), userSummary.characterType()),
                stance,
                comment.getContent(),
                0,
                false,
                true,
                comment.getCreatedAt()
        );
    }

    public CommentListResponse getComments(Long perspectiveId, Long userId, String cursor, Integer size) {
        Perspective perspective = findPerspectiveById(perspectiveId);

        int pageSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : size;
        PageRequest pageable = PageRequest.of(0, pageSize);

        List<PerspectiveComment> comments = cursor == null
                ? commentRepository.findByPerspectiveOrderByCreatedAtDesc(perspective, pageable)
                : commentRepository.findByPerspectiveAndCreatedAtBeforeOrderByCreatedAtDesc(
                        perspective, LocalDateTime.parse(cursor), pageable);

        Long battleId = perspective.getBattle().getId();
        List<CommentListResponse.Item> items = comments.stream()
                .filter(c -> !c.isHidden())
                .map(c -> {
                    UserSummary user = userQueryService.findSummaryById(c.getUser().getId());
                    Long postVoteOptionId = voteService.findPostVoteOptionId(battleId, c.getUser().getId());
                    String stance = null;
                    if (postVoteOptionId != null) {
                        BattleOption option = battleService.findOptionById(postVoteOptionId);
                        stance = option.getStance();
                    }
                    boolean isLiked = commentLikeRepository.existsByCommentAndUserId(c, userId);
                    return new CommentListResponse.Item(
                            c.getId(),
                            new CommentListResponse.UserSummary(user.userTag(), user.nickname(), user.characterType()),
                            stance,
                            c.getContent(),
                            c.getLikeCount(),
                            isLiked,
                            c.getUser().getId().equals(userId),
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
    public void deleteComment(Long perspectiveId, Long commentId, Long userId) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        PerspectiveComment comment = findCommentById(commentId);
        validateOwnership(comment, userId);

        commentRepository.delete(comment);
        perspective.decrementCommentCount();
    }

    @Transactional
    public UpdateCommentResponse updateComment(Long perspectiveId, Long commentId, Long userId, UpdateCommentRequest request) {
        findPerspectiveById(perspectiveId);
        PerspectiveComment comment = findCommentById(commentId);
        validateOwnership(comment, userId);

        comment.updateContent(request.content());
        return new UpdateCommentResponse(comment.getId(), comment.getContent(), comment.getUpdatedAt());
    }

    private Perspective findPerspectiveById(Long perspectiveId) {
        return perspectiveRepository.findById(perspectiveId)
                .orElseThrow(() -> new CustomException(ErrorCode.PERSPECTIVE_NOT_FOUND));
    }

    private PerspectiveComment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateOwnership(PerspectiveComment comment, Long userId) {
        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMENT_FORBIDDEN);
        }
    }
}
