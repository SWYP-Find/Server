package com.swyp.picke.domain.perspective.service;

import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.service.ClassAccessService;
import com.swyp.picke.domain.perspective.entity.CommentReport;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.perspective.entity.PerspectiveReport;
import com.swyp.picke.domain.perspective.repository.CommentReportRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveReportRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클래스 의견·댓글은 클래스 읽기 권한(운영자·참여 중인 멤버)이 있어야 신고할 수 있다.
 * 볼 수 있는 글은 마감 후에도 신고할 수 있도록 쓰기 권한이 아닌 읽기 권한으로 판정한다.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int REPORT_THRESHOLD = 10;

    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveCommentRepository commentRepository;
    private final PerspectiveReportRepository perspectiveReportRepository;
    private final CommentReportRepository commentReportRepository;
    private final UserRepository userRepository;
    private final ClassAccessService classAccessService;

    @Transactional
    public void reportPerspective(Long perspectiveId, Long userId) {
        Perspective perspective = perspectiveRepository.findById(perspectiveId)
                .orElseThrow(() -> new CustomException(ErrorCode.PERSPECTIVE_NOT_FOUND));
        requireClassReadable(perspective.getClassroom(), userId);
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
    public void reportComment(Long perspectiveId, Long commentId, Long userId) {
        PerspectiveComment comment = commentRepository.findById(commentId)
                .filter(c -> c.getPerspective().getId().equals(perspectiveId))
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        requireClassReadable(comment.getPerspective().getClassroom(), userId);
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

    private void requireClassReadable(Classroom classroom, Long userId) {
        if (classroom != null) {
            classAccessService.requireReadable(classroom, userId);
        }
    }
}
