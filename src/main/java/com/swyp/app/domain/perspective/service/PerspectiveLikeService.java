package com.swyp.app.domain.perspective.service;

import com.swyp.app.domain.perspective.dto.response.LikeCountResponse;
import com.swyp.app.domain.perspective.dto.response.LikeResponse;
import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveLike;
import com.swyp.app.domain.perspective.repository.PerspectiveLikeRepository;
import com.swyp.app.domain.perspective.repository.PerspectiveRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerspectiveLikeService {

    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveLikeRepository likeRepository;

    public LikeCountResponse getLikeCount(Long perspectiveId) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        long likeCount = likeRepository.countByPerspective(perspective);
        return new LikeCountResponse(perspective.getId(), likeCount);
    }

    @Transactional
    public LikeResponse addLike(Long perspectiveId, Long userId) {
        Perspective perspective = findPerspectiveById(perspectiveId);

        if (perspective.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.LIKE_SELF_FORBIDDEN);
        }

        if (likeRepository.existsByPerspectiveAndUserId(perspective, userId)) {
            throw new CustomException(ErrorCode.LIKE_ALREADY_EXISTS);
        }

        likeRepository.save(PerspectiveLike.builder()
                .perspective(perspective)
                .userId(userId)
                .build());
        perspective.incrementLikeCount();

        return new LikeResponse(perspective.getId(), perspective.getLikeCount(), true);
    }

    @Transactional
    public LikeResponse removeLike(Long perspectiveId, Long userId) {
        Perspective perspective = findPerspectiveById(perspectiveId);

        PerspectiveLike like = likeRepository.findByPerspectiveAndUserId(perspective, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.LIKE_NOT_FOUND));

        likeRepository.delete(like);
        perspective.decrementLikeCount();

        return new LikeResponse(perspective.getId(), perspective.getLikeCount(), false);
    }

    private Perspective findPerspectiveById(Long perspectiveId) {
        return perspectiveRepository.findById(perspectiveId)
                .orElseThrow(() -> new CustomException(ErrorCode.PERSPECTIVE_NOT_FOUND));
    }
}
