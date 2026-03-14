package com.swyp.app.domain.perspective.service;

import com.swyp.app.domain.perspective.dto.request.CreatePerspectiveRequest;
import com.swyp.app.domain.perspective.dto.request.UpdatePerspectiveRequest;
import com.swyp.app.domain.perspective.dto.response.CreatePerspectiveResponse;
import com.swyp.app.domain.perspective.dto.response.PerspectiveListResponse;
import com.swyp.app.domain.perspective.dto.response.UpdatePerspectiveResponse;
import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.repository.PerspectiveLikeRepository;
import com.swyp.app.domain.perspective.repository.PerspectiveRepository;
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
public class PerspectiveService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveLikeRepository perspectiveLikeRepository;

    @Transactional
    public CreatePerspectiveResponse createPerspective(UUID battleId, Long userId, CreatePerspectiveRequest request) {
        // TODO: Battle 엔티티 병합 후 배틀 존재 여부 검증 (ErrorCode.BATTLE_NOT_FOUND)
        // TODO: Vote 엔티티 병합 후 사전 투표 여부 검증 및 optionId 조회

        if (perspectiveRepository.existsByBattleIdAndUserId(battleId, userId)) {
            throw new CustomException(ErrorCode.PERSPECTIVE_ALREADY_EXISTS);
        }

        Perspective perspective = Perspective.builder()
                .battleId(battleId)
                .userId(userId)
                .optionId(null) // TODO: Vote에서 조회한 optionId로 교체
                .content(request.content())
                .build();

        Perspective saved = perspectiveRepository.save(perspective);
        return new CreatePerspectiveResponse(saved.getId(), saved.getStatus(), saved.getCreatedAt());
    }

    public PerspectiveListResponse getPerspectives(UUID battleId, String cursor, Integer size, String optionLabel) {
        // TODO: Battle 엔티티 병합 후 배틀 존재 여부 검증 (ErrorCode.BATTLE_NOT_FOUND)
        // TODO: BattleOption 병합 후 optionLabel → optionId 변환 및 필터 쿼리 적용

        int pageSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : size;
        PageRequest pageable = PageRequest.of(0, pageSize);

        List<Perspective> perspectives = cursor == null
                ? perspectiveRepository.findByBattleIdOrderByCreatedAtDesc(battleId, pageable)
                : perspectiveRepository.findByBattleIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                        battleId, LocalDateTime.parse(cursor), pageable);

        // TODO: User, BattleOption 병합 후 user/option 정보 실제 데이터로 교체
        List<PerspectiveListResponse.Item> items = perspectives.stream()
                .map(p -> new PerspectiveListResponse.Item(
                        p.getId(),
                        new PerspectiveListResponse.UserSummary(null, null, null),
                        new PerspectiveListResponse.OptionSummary(p.getOptionId(), null, null),
                        p.getContent(),
                        p.getLikeCount(),
                        p.getCommentCount(),
                        false, // TODO: 현재 로그인 유저 기반 좋아요 여부로 교체
                        p.getCreatedAt()
                ))
                .toList();

        String nextCursor = perspectives.size() == pageSize
                ? perspectives.get(perspectives.size() - 1).getCreatedAt().toString()
                : null;

        return new PerspectiveListResponse(items, nextCursor, nextCursor != null);
    }

    @Transactional
    public void deletePerspective(UUID perspectiveId, Long userId) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        validateOwnership(perspective, userId);
        perspectiveRepository.delete(perspective);
    }

    @Transactional
    public UpdatePerspectiveResponse updatePerspective(UUID perspectiveId, Long userId, UpdatePerspectiveRequest request) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        validateOwnership(perspective, userId);
        perspective.updateContent(request.content());
        return new UpdatePerspectiveResponse(perspective.getId(), perspective.getContent(), perspective.getUpdatedAt());
    }

    private Perspective findPerspectiveById(UUID perspectiveId) {
        return perspectiveRepository.findById(perspectiveId)
                .orElseThrow(() -> new CustomException(ErrorCode.PERSPECTIVE_NOT_FOUND));
    }

    private void validateOwnership(Perspective perspective, Long userId) {
        if (!perspective.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.PERSPECTIVE_FORBIDDEN);
        }
    }
}
