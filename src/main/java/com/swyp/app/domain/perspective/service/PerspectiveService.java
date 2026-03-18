package com.swyp.app.domain.perspective.service;

import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.domain.perspective.entity.PerspectiveStatus;
import com.swyp.app.domain.perspective.dto.request.CreatePerspectiveRequest;
import com.swyp.app.domain.perspective.dto.request.UpdatePerspectiveRequest;
import com.swyp.app.domain.perspective.dto.response.CreatePerspectiveResponse;
import com.swyp.app.domain.perspective.dto.response.MyPerspectiveResponse;
import com.swyp.app.domain.perspective.dto.response.PerspectiveListResponse;
import com.swyp.app.domain.perspective.dto.response.UpdatePerspectiveResponse;
import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.repository.PerspectiveLikeRepository;
import com.swyp.app.domain.perspective.repository.PerspectiveRepository;
import com.swyp.app.domain.user.dto.response.UserSummary;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerspectiveService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveLikeRepository perspectiveLikeRepository;
    private final BattleService battleService;
    private final VoteService voteService;
    private final UserService userQueryService;
    private final GptModerationService gptModerationService;

    @Transactional
    public CreatePerspectiveResponse createPerspective(UUID battleId, Long userId, CreatePerspectiveRequest request) {
        battleService.findById(battleId);

        if (perspectiveRepository.existsByBattleIdAndUserId(battleId, userId)) {
            throw new CustomException(ErrorCode.PERSPECTIVE_ALREADY_EXISTS);
        }

        UUID optionId = voteService.findPreVoteOptionId(battleId, userId);

        Perspective perspective = Perspective.builder()
                .battleId(battleId)
                .userId(userId)
                .optionId(optionId)
                .content(request.content())
                .build();

        Perspective saved = perspectiveRepository.save(perspective);
        gptModerationService.moderate(saved.getId(), saved.getContent());
        return new CreatePerspectiveResponse(saved.getId(), saved.getStatus(), saved.getCreatedAt());
    }

    public PerspectiveListResponse getPerspectives(UUID battleId, Long userId, String cursor, Integer size, String optionLabel) {
        battleService.findById(battleId);

        int pageSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : size;
        PageRequest pageable = PageRequest.of(0, pageSize);

        List<Perspective> perspectives;

        if (optionLabel != null) {
            BattleOptionLabel label = BattleOptionLabel.valueOf(optionLabel.toUpperCase());
            BattleOption option = battleService.findOptionByBattleIdAndLabel(battleId, label);
            perspectives = cursor == null
                    ? perspectiveRepository.findByBattleIdAndOptionIdAndStatusOrderByCreatedAtDesc(battleId, option.getId(), PerspectiveStatus.PUBLISHED, pageable)
                    : perspectiveRepository.findByBattleIdAndOptionIdAndStatusAndCreatedAtBeforeOrderByCreatedAtDesc(battleId, option.getId(), PerspectiveStatus.PUBLISHED, LocalDateTime.parse(cursor), pageable);
        } else {
            perspectives = cursor == null
                    ? perspectiveRepository.findByBattleIdAndStatusOrderByCreatedAtDesc(battleId, PerspectiveStatus.PUBLISHED, pageable)
                    : perspectiveRepository.findByBattleIdAndStatusAndCreatedAtBeforeOrderByCreatedAtDesc(battleId, PerspectiveStatus.PUBLISHED, LocalDateTime.parse(cursor), pageable);
        }

        List<PerspectiveListResponse.Item> items = perspectives.stream()
                .map(p -> {
                    UserSummary user = userQueryService.findSummaryById(p.getUserId());
                    BattleOption option = battleService.findOptionById(p.getOptionId());
                    boolean isLiked = perspectiveLikeRepository.existsByPerspectiveAndUserId(p, userId);
                    return new PerspectiveListResponse.Item(
                            p.getId(),
                            new PerspectiveListResponse.UserSummary(user.userTag(), user.nickname(), user.characterType()),
                            new PerspectiveListResponse.OptionSummary(option.getId(), option.getLabel().name(), option.getTitle()),
                            p.getContent(),
                            p.getLikeCount(),
                            p.getCommentCount(),
                            isLiked,
                            p.getCreatedAt()
                    );
                })
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
        perspective.updateStatus(PerspectiveStatus.PENDING);
        gptModerationService.moderate(perspective.getId(), perspective.getContent());
        return new UpdatePerspectiveResponse(perspective.getId(), perspective.getContent(), perspective.getUpdatedAt());
    }

    public MyPerspectiveResponse getMyPendingPerspective(UUID battleId, Long userId) {
        battleService.findById(battleId);
        Perspective perspective = perspectiveRepository.findByBattleIdAndUserId(battleId, userId)
                .filter(p -> p.getStatus() == PerspectiveStatus.PENDING)
                .orElseThrow(() -> new CustomException(ErrorCode.PERSPECTIVE_NOT_FOUND));
        return new MyPerspectiveResponse(
                perspective.getId(),
                perspective.getContent(),
                perspective.getStatus(),
                perspective.getCreatedAt()
        );
    }

    @Transactional
    public void retryModeration(UUID perspectiveId, Long userId) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        validateOwnership(perspective, userId);
        if (perspective.getStatus() != PerspectiveStatus.MODERATION_FAILED) {
            throw new CustomException(ErrorCode.PERSPECTIVE_MODERATION_NOT_FAILED);
        }
        perspective.updateStatus(PerspectiveStatus.PENDING);
        gptModerationService.moderate(perspectiveId, perspective.getContent());
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
