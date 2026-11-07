package com.swyp.picke.domain.perspective.service;

import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.service.ClassAccessService;
import com.swyp.picke.domain.notification.service.NotificationDispatchService;
import com.swyp.picke.domain.perspective.dto.response.LikeResponse;
import com.swyp.picke.domain.perspective.entity.CommentLike;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.perspective.repository.CommentLikeRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 클래스 댓글이면 좋아요 등록·취소에 클래스 쓰기 권한(참여 중인 멤버 + 마감 전)을 검사한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeService {

    private final PerspectiveCommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final ClassAccessService classAccessService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Transactional
    public LikeResponse addLike(Long commentId, Long userId) {
        PerspectiveComment comment = findCommentById(commentId);
        requireClassWritable(comment, userId);

        if (comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.LIKE_SELF_FORBIDDEN);
        }

        if (commentLikeRepository.existsByCommentAndUserId(comment, userId)) {
            throw new CustomException(ErrorCode.LIKE_ALREADY_EXISTS);
        }

        commentLikeRepository.save(CommentLike.builder()
                .comment(comment)
                .userId(userId)
                .build());
        comment.incrementLikeCount();

        notifyCommentLikeAfterCommit(comment);

        return new LikeResponse(comment.getId(), comment.getLikeCount(), true);
    }

    private void notifyCommentLikeAfterCommit(PerspectiveComment comment) {
        Long commentId = comment.getId();
        Long commentAuthorId = comment.getUser().getId();
        Long perspectiveId = comment.getPerspective().getId();
        String battleTitle = comment.getPerspective().getBattle().getTitle();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationDispatchService.notifyCommentLike(commentAuthorId, perspectiveId, commentId, battleTitle);
                }
            });
            return;
        }

        notificationDispatchService.notifyCommentLike(commentAuthorId, perspectiveId, commentId, battleTitle);
    }

    @Transactional
    public LikeResponse removeLike(Long commentId, Long userId) {
        PerspectiveComment comment = findCommentById(commentId);
        requireClassWritable(comment, userId);

        CommentLike like = commentLikeRepository.findByCommentAndUserId(comment, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.LIKE_NOT_FOUND));

        commentLikeRepository.delete(like);
        comment.decrementLikeCount();

        return new LikeResponse(comment.getId(), comment.getLikeCount(), false);
    }

    private void requireClassWritable(PerspectiveComment comment, Long userId) {
        Classroom classroom = comment.getPerspective().getClassroom();
        if (classroom != null) {
            classAccessService.requireWritable(classroom, userId, LocalDateTime.now(KST));
        }
    }

    private PerspectiveComment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }
}
