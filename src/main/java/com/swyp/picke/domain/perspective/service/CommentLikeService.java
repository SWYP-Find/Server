package com.swyp.picke.domain.perspective.service;

import com.swyp.picke.domain.perspective.dto.response.LikeResponse;
import com.swyp.picke.domain.perspective.entity.CommentLike;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.perspective.repository.CommentLikeRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeService {

    private final PerspectiveCommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    @Transactional
    public LikeResponse addLike(Long commentId, Long userId) {
        PerspectiveComment comment = findCommentById(commentId);

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

        return new LikeResponse(comment.getId(), comment.getLikeCount(), true);
    }

    @Transactional
    public LikeResponse removeLike(Long commentId, Long userId) {
        PerspectiveComment comment = findCommentById(commentId);

        CommentLike like = commentLikeRepository.findByCommentAndUserId(comment, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.LIKE_NOT_FOUND));

        commentLikeRepository.delete(like);
        comment.decrementLikeCount();

        return new LikeResponse(comment.getId(), comment.getLikeCount(), false);
    }

    private PerspectiveComment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }
}
