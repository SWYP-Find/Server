package com.swyp.picke.domain.perspective.service;

import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.battle.util.BattleOptionDisplay;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.service.ClassAccessService;
import com.swyp.picke.domain.classroom.service.ClassDisplayNameResolver;
import com.swyp.picke.domain.notification.service.NotificationDispatchService;
import com.swyp.picke.domain.perspective.dto.request.CreateCommentRequest;
import com.swyp.picke.domain.perspective.dto.request.UpdateCommentRequest;
import com.swyp.picke.domain.perspective.dto.response.CommentListResponse;
import com.swyp.picke.domain.perspective.dto.response.CreateCommentResponse;
import com.swyp.picke.domain.perspective.dto.response.UpdateCommentResponse;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.perspective.repository.CommentLikeRepository;
import com.swyp.picke.domain.perspective.repository.CommentReportRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.user.dto.response.UserSummary;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.domain.vote.service.BattleVoteService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 의견 댓글 서비스. 댓글은 부모 의견의 class_id 를 그대로 물려받는다.
 * 클래스 의견의 댓글은 운영자/ACTIVE 멤버만 읽고, ACTIVE 멤버가 마감 전에만 쓴다 (PRD §15, 피그마 "참여가 마감되어 댓글을 등록할 수 없어요").
 * 클래스 댓글은 일반 알림(새 답글)·마이페이지 집계에 섞이지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerspectiveCommentService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentReportRepository commentReportRepository;
    private final UserService userQueryService;
    private final BattleVoteService BattleVoteService;
    private final BattleService battleService;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final NotificationDispatchService notificationDispatchService;
    private final ClassAccessService classAccessService;
    private final ClassDisplayNameResolver classDisplayNameResolver;

    @Transactional
    public CreateCommentResponse createComment(Long perspectiveId, Long userId, CreateCommentRequest request) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        Classroom classroom = perspective.getClassroom();
        requireClassWritable(classroom, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        PerspectiveComment comment = PerspectiveComment.builder()
                .perspective(perspective)
                .user(user)
                .classroom(classroom)
                .content(request.content())
                .build();

        commentRepository.save(comment);
        perspective.incrementCommentCount();

        if (classroom == null) {
            notifyNewCommentAfterCommit(comment, perspective, userId);
        }

        UserSummary userSummary = userQueryService.findSummaryById(userId);
        String characterImageUrl = resolveCharacterImageUrl(userSummary.characterType());
        String stance = resolveStance(perspective.getBattle().getId(), userId, classroom);
        String name = classDisplayNameResolver.nameOf(classroom, userId, userSummary.nickname());
        return new CreateCommentResponse(
                comment.getId(),
                new CreateCommentResponse.UserSummary(userSummary.userTag(), name, userSummary.characterType(), characterImageUrl),
                stance,
                comment.getContent(),
                0,
                false,
                true,
                comment.getCreatedAt()
        );
    }

    private void notifyNewCommentAfterCommit(PerspectiveComment comment, Perspective perspective, Long commenterId) {
        Long perspectiveAuthorId = perspective.getUser().getId();
        if (perspectiveAuthorId.equals(commenterId)) {
            return;
        }

        Long commentId = comment.getId();
        Long perspectiveId = perspective.getId();
        String battleTitle = perspective.getBattle().getTitle();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationDispatchService.notifyNewComment(perspectiveAuthorId, perspectiveId, commentId, battleTitle);
                }
            });
            return;
        }

        notificationDispatchService.notifyNewComment(perspectiveAuthorId, perspectiveId, commentId, battleTitle);
    }

    public CommentListResponse getComments(Long perspectiveId, Long userId, String cursor, Integer size) {
        return listComments(perspectiveId, userId, cursor, size);
    }

    public CommentListResponse getCommentsWithLabel(Long perspectiveId, Long userId, String cursor, Integer size) {
        return listComments(perspectiveId, userId, cursor, size);
    }

    private CommentListResponse listComments(Long perspectiveId, Long userId, String cursor, Integer size) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        Classroom classroom = perspective.getClassroom();
        requireClassReadable(classroom, userId);

        int pageSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : size;
        PageRequest pageable = PageRequest.of(0, pageSize);

        List<PerspectiveComment> comments = cursor == null
                ? commentRepository.findByPerspectiveOrderByCreatedAtDesc(perspective, pageable)
                : commentRepository.findByPerspectiveAndCreatedAtBeforeOrderByCreatedAtDesc(
                        perspective, LocalDateTime.parse(cursor), pageable);

        Long battleId = perspective.getBattle().getId();
        // 클래스 댓글이면 작성자를 클래스 표시 이름으로 보여준다
        Map<Long, String> classNames = classDisplayNameResolver.resolve(classroom,
                comments.stream().map(c -> c.getUser().getId()).distinct().toList());
        List<CommentListResponse.Item> items = comments.stream()
                .filter(c -> !c.isHidden())
                .map(c -> {
                    UserSummary user = userQueryService.findSummaryById(c.getUser().getId());
                    String characterImageUrl = resolveCharacterImageUrl(user.characterType());
                    String name = classNames.getOrDefault(c.getUser().getId(), user.nickname());
                    String stance = resolveStance(battleId, c.getUser().getId(), classroom);
                    boolean isLiked = commentLikeRepository.existsByCommentAndUserId(c, userId);
                    return new CommentListResponse.Item(
                            c.getId(),
                            new CommentListResponse.UserSummary(user.userTag(), name, user.characterType(), characterImageUrl),
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
        requireClassWritable(perspective.getClassroom(), userId);

        commentLikeRepository.deleteAllByComment(comment);
        commentReportRepository.deleteAllByComment(comment);
        commentRepository.delete(comment);
        perspective.decrementCommentCount();
    }

    @Transactional
    public UpdateCommentResponse updateComment(Long perspectiveId, Long commentId, Long userId, UpdateCommentRequest request) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        PerspectiveComment comment = findCommentById(commentId);
        validateOwnership(comment, userId);
        requireClassWritable(perspective.getClassroom(), userId);

        comment.updateContent(request.content());
        return new UpdateCommentResponse(comment.getId(), comment.getContent(), comment.getUpdatedAt());
    }

    /** 댓글 작성자의 사후투표 입장. 클래스 의견이면 같은 클래스 안의 투표만 본다 */
    private String resolveStance(Long battleId, Long authorId, Classroom classroom) {
        Long postVoteOptionId = BattleVoteService.findPostVoteOptionId(battleId, authorId, classroom);
        if (postVoteOptionId == null) {
            return null;
        }
        BattleOption option = battleService.findOptionById(postVoteOptionId);
        return BattleOptionDisplay.opinion(option);
    }

    private void requireClassReadable(Classroom classroom, Long userId) {
        if (classroom != null) {
            classAccessService.requireReadable(classroom, userId);
        }
    }

    private void requireClassWritable(Classroom classroom, Long userId) {
        if (classroom != null) {
            classAccessService.requireWritable(classroom, userId, LocalDateTime.now(KST));
        }
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

    private String resolveCharacterImageUrl(String characterType) {
        if (characterType == null || characterType.isBlank()) {
            return null;
        }
        return s3PresignedUrlService.generatePresignedUrl(CharacterType.resolveImageKey(characterType));
    }
}
