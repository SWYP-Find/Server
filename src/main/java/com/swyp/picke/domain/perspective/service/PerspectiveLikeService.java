package com.swyp.picke.domain.perspective.service;

import com.swyp.picke.domain.classroom.service.ClassAccessService;
import com.swyp.picke.domain.perspective.dto.response.LikeCountResponse;
import com.swyp.picke.domain.perspective.dto.response.LikeResponse;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveLike;
import com.swyp.picke.domain.perspective.repository.PerspectiveLikeRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 클래스 의견이면 좋아요 수 조회는 클래스 읽기 권한, 등록·취소는 쓰기 권한(참여 중인 멤버 + 마감 전)을 검사한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerspectiveLikeService {

    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ClassAccessService classAccessService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public LikeCountResponse getLikeCount(Long perspectiveId, Long userId) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        if (perspective.getClassroom() != null) {
            classAccessService.requireReadable(perspective.getClassroom(), userId);
        }
        long likeCount = likeRepository.countByPerspective(perspective);
        return new LikeCountResponse(perspective.getId(), likeCount);
    }

    @Transactional
    public LikeResponse addLike(Long perspectiveId, Long userId) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        requireClassWritable(perspective, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (perspective.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.LIKE_SELF_FORBIDDEN);
        }

        if (likeRepository.existsByPerspectiveAndUserId(perspective, userId)) {
            throw new CustomException(ErrorCode.LIKE_ALREADY_EXISTS);
        }

        likeRepository.save(PerspectiveLike.builder()
                .perspective(perspective)
                .user(user)
                .build());
        perspective.incrementLikeCount();

        return new LikeResponse(perspective.getId(), perspective.getLikeCount(), true);
    }

    @Transactional
    public LikeResponse removeLike(Long perspectiveId, Long userId) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        requireClassWritable(perspective, userId);

        PerspectiveLike like = likeRepository.findByPerspectiveAndUserId(perspective, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.LIKE_NOT_FOUND));

        likeRepository.delete(like);
        perspective.decrementLikeCount();

        return new LikeResponse(perspective.getId(), perspective.getLikeCount(), false);
    }

    private void requireClassWritable(Perspective perspective, Long userId) {
        if (perspective.getClassroom() != null) {
            classAccessService.requireWritable(perspective.getClassroom(), userId, LocalDateTime.now(KST));
        }
    }

    private Perspective findPerspectiveById(Long perspectiveId) {
        return perspectiveRepository.findById(perspectiveId)
                .orElseThrow(() -> new CustomException(ErrorCode.PERSPECTIVE_NOT_FOUND));
    }
}
