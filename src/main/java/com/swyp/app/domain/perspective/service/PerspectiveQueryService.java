package com.swyp.app.domain.perspective.service;

import com.swyp.app.domain.perspective.entity.PerspectiveComment;
import com.swyp.app.domain.perspective.entity.PerspectiveLike;
import com.swyp.app.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.app.domain.perspective.repository.PerspectiveLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerspectiveQueryService {

    private final PerspectiveCommentRepository perspectiveCommentRepository;
    private final PerspectiveLikeRepository perspectiveLikeRepository;

    public List<PerspectiveComment> findUserComments(Long userId, int offset, int size) {
        PageRequest pageable = PageRequest.of(offset / size, size);
        return perspectiveCommentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long countUserComments(Long userId) {
        return perspectiveCommentRepository.countByUserId(userId);
    }

    public List<PerspectiveLike> findUserLikes(Long userId, int offset, int size) {
        PageRequest pageable = PageRequest.of(offset / size, size);
        return perspectiveLikeRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long countUserLikes(Long userId) {
        return perspectiveLikeRepository.countByUserId(userId);
    }
}
