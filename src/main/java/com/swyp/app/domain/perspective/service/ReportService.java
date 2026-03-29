package com.swyp.app.domain.perspective.service;

import com.swyp.app.domain.perspective.entity.CommentReport;
import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveComment;
import com.swyp.app.domain.perspective.entity.PerspectiveReport;
import com.swyp.app.domain.perspective.repository.CommentReportRepository;
import com.swyp.app.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.app.domain.perspective.repository.PerspectiveReportRepository;
import com.swyp.app.domain.perspective.repository.PerspectiveRepository;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int REPORT_THRESHOLD = 10;

    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveCommentRepository commentRepository;
    private final PerspectiveReportRepository perspectiveReportRepository;
    private final CommentReportRepository commentReportRepository;
    private final UserRepository userRepository;

    @Transactional
    public void reportPerspective(Long perspectiveId, Long userId) {
        Perspective perspective = perspectiveRepository.findById(perspectiveId)
                .orElseThrow(() -> new CustomException(ErrorCode.PERSPECTIVE_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (perspective.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.REPORT_SELF_FORBIDDEN);
        }
        if (perspectiveReportRepository.existsByPerspectiveAndUserId(perspective, userId)) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_EXISTS);
        }

        perspectiveReportRepository.save(PerspectiveReport.builder()
                .perspective(perspective)
                .user(user)
                .build());

        long reportCount = perspectiveReportRepository.countByPerspective(perspective);
        if (reportCount >= REPORT_THRESHOLD) {
            perspective.hide();
        }
    }

    @Transactional
    public void reportComment(Long commentId, Long userId) {
        PerspectiveComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.REPORT_SELF_FORBIDDEN);
        }
        if (commentReportRepository.existsByCommentAndUserId(comment, userId)) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_EXISTS);
        }

        commentReportRepository.save(CommentReport.builder()
                .comment(comment)
                .user(user)
                .build());

        long reportCount = commentReportRepository.countByComment(comment);
        if (reportCount >= REPORT_THRESHOLD) {
            comment.hide();
        }
    }
}
