package com.swyp.picke.domain.perspective.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.battle.util.BattleOptionDisplay;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.service.ClassAccessService;
import com.swyp.picke.domain.classroom.service.ClassDisplayNameResolver;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.perspective.dto.request.CreatePerspectiveRequest;
import com.swyp.picke.domain.perspective.dto.request.UpdatePerspectiveRequest;
import com.swyp.picke.domain.perspective.dto.response.CreatePerspectiveResponse;
import com.swyp.picke.domain.perspective.dto.response.MyPerspectiveResponse;
import com.swyp.picke.domain.perspective.dto.response.PerspectiveDetailResponse;
import com.swyp.picke.domain.perspective.dto.response.PerspectiveListResponse;
import com.swyp.picke.domain.perspective.dto.response.UpdatePerspectiveResponse;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.perspective.repository.CommentLikeRepository;
import com.swyp.picke.domain.perspective.repository.CommentReportRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveLikeRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveReportRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.user.dto.response.UserSummary;
import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.domain.vote.service.BattleVoteService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.common.util.CursorParser;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 의견(Perspective) 서비스.
 * classroom 인자가 없는 메서드는 일반 배틀(class_id IS NULL)만 다루고,
 * classroom 을 받는 메서드는 해당 클래스 안의 의견만 다룬다.
 * 의견 단건(상세·수정·삭제)은 저장된 class_id 로 클래스 여부를 판단해 멤버 권한·마감을 검사한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerspectiveService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveCommentRepository perspectiveCommentRepository;
    private final PerspectiveLikeRepository perspectiveLikeRepository;
    private final PerspectiveReportRepository perspectiveReportRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentReportRepository commentReportRepository;
    private final BattleService battleService;
    private final BattleVoteService BattleVoteService;
    private final UserService userQueryService;
    private final UserRepository userRepository;
    private final GptModerationService gptModerationService;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final ClassAccessService classAccessService;
    private final ClassDisplayNameResolver classDisplayNameResolver;

    public PerspectiveDetailResponse getPerspectiveDetail(Long perspectiveId, Long userId) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        if (perspective.getStatus() == PerspectiveStatus.HIDDEN) {
            throw new CustomException(ErrorCode.PERSPECTIVE_NOT_FOUND);
        }
        requireClassReadable(perspective, userId);
        Classroom classroom = perspective.getClassroom();
        // 나간·내보낸 멤버의 클래스 의견은 숨긴다
        if (!classDisplayNameResolver.isVisibleAuthor(classroom, perspective.getUser().getId())) {
            throw new CustomException(ErrorCode.PERSPECTIVE_NOT_FOUND);
        }
        UserSummary user = userQueryService.findSummaryById(perspective.getUser().getId());
        String characterImageUrl = resolveCharacterImageUrl(user.characterType());
        String name = classDisplayNameResolver.nameOf(classroom, perspective.getUser().getId(), user.nickname());
        BattleOption option = perspective.getOption();
        boolean isLiked = perspectiveLikeRepository.existsByPerspectiveAndUserId(perspective, userId);
        return new PerspectiveDetailResponse(
                perspective.getId(),
                new PerspectiveDetailResponse.UserSummary(ClassDisplayNameResolver.userTagOf(classroom, user.userTag()), name, user.characterType(), characterImageUrl),
                new PerspectiveDetailResponse.OptionSummary(option.getId(), BattleOptionDisplay.opinion(option), option.getTitle(), option.getStance()),
                perspective.getContent(),
                perspective.getLikeCount(),
                perspective.getCommentCount(),
                isLiked,
                perspective.getUser().getId().equals(userId),
                perspective.getCreatedAt()
        );
    }

    @Transactional
    public CreatePerspectiveResponse createPerspective(Long battleId, Long userId, CreatePerspectiveRequest request) {
        return createPerspective(battleId, userId, null, request);
    }

    /**
     * 의견 등록. 사후투표한 선택지에 귀속된다.
     * 클래스 의견은 호출자(ClassBattleService)가 멤버·마감 검사를 끝낸 뒤 classroom 을 넘긴다.
     */
    @Transactional
    public CreatePerspectiveResponse createPerspective(Long battleId, Long userId, Classroom classroom, CreatePerspectiveRequest request) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        Long postVoteOptionId = BattleVoteService.findPostVoteOptionId(battleId, userId, classroom);
        BattleOption option = battleService.findOptionById(postVoteOptionId);

        Perspective perspective = Perspective.builder()
                .battle(battle)
                .user(user)
                .option(option)
                .classroom(classroom)
                .content(request.content())
                .build();

        perspective.updateStatus(PerspectiveStatus.PUBLISHED);
        Perspective saved = perspectiveRepository.save(perspective);
        return new CreatePerspectiveResponse(saved.getId(), saved.getStatus(), saved.getCreatedAt());
    }

    public PerspectiveListResponse getPerspectives(Long battleId, Long userId, String cursor, Integer size, Long optionId, String sort) {
        return getPerspectives(battleId, userId, null, cursor, size, optionId, sort);
    }

    public PerspectiveListResponse getPerspectives(Long battleId, Long userId, Classroom classroom,
                                                   String cursor, Integer size, Long optionId, String sort) {
        Battle battle = battleService.findById(battleId);

        int pageSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : size;
        PageRequest pageable = PageRequest.of(0, pageSize);

        boolean isPopular = "popular".equalsIgnoreCase(sort);
        List<Perspective> perspectives;

        if (optionId != null) {
            BattleOption option = findFilterOption(battle, optionId);
            perspectives = isPopular
                    ? perspectiveRepository.findByBattleIdAndClassroomAndOptionIdAndStatusOrderByLikeCountDescCreatedAtDesc(battleId, classroom, option.getId(), PerspectiveStatus.PUBLISHED, pageable)
                    : cursor == null
                        ? perspectiveRepository.findByBattleIdAndClassroomAndOptionIdAndStatusOrderByCreatedAtDesc(battleId, classroom, option.getId(), PerspectiveStatus.PUBLISHED, pageable)
                        : perspectiveRepository.findByBattleIdAndClassroomAndOptionIdAndStatusAndCreatedAtBeforeOrderByCreatedAtDesc(battleId, classroom, option.getId(), PerspectiveStatus.PUBLISHED, CursorParser.parseDateTime(cursor), pageable);
        } else {
            perspectives = isPopular
                    ? perspectiveRepository.findByBattleIdAndClassroomAndStatusOrderByLikeCountDescCreatedAtDesc(battleId, classroom, PerspectiveStatus.PUBLISHED, pageable)
                    : cursor == null
                        ? perspectiveRepository.findByBattleIdAndClassroomAndStatusOrderByCreatedAtDesc(battleId, classroom, PerspectiveStatus.PUBLISHED, pageable)
                        : perspectiveRepository.findByBattleIdAndClassroomAndStatusAndCreatedAtBeforeOrderByCreatedAtDesc(battleId, classroom, PerspectiveStatus.PUBLISHED, CursorParser.parseDateTime(cursor), pageable);
        }

        // 클래스 의견이면 작성자를 클래스 표시 이름으로 보여주고, 나간·내보낸 멤버의 의견은 뺀다
        Map<Long, String> classNames = classDisplayNameResolver.resolve(classroom,
                perspectives.stream().map(p -> p.getUser().getId()).distinct().toList());
        List<PerspectiveListResponse.Item> items = perspectives.stream()
                .filter(p -> ClassDisplayNameResolver.isVisible(classroom, classNames, p.getUser().getId()))
                .map(p -> {
                    UserSummary user = userQueryService.findSummaryById(p.getUser().getId());
                    String characterImageUrl = resolveCharacterImageUrl(user.characterType());
                    String name = classNames.getOrDefault(p.getUser().getId(), user.nickname());
                    BattleOption option = p.getOption();
                    boolean isLiked = perspectiveLikeRepository.existsByPerspectiveAndUserId(p, userId);
                    return new PerspectiveListResponse.Item(
                            p.getId(),
                            new PerspectiveListResponse.UserSummary(ClassDisplayNameResolver.userTagOf(classroom, user.userTag()), name, user.characterType(), characterImageUrl),
                            new PerspectiveListResponse.OptionSummary(option.getId(), BattleOptionDisplay.opinion(option), option.getTitle(), option.getStance()),
                            p.getContent(),
                            p.getLikeCount(),
                            p.getCommentCount(),
                            isLiked,
                            p.getUser().getId().equals(userId),
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
    public void deletePerspective(Long perspectiveId, Long userId) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        validateOwnership(perspective, userId);
        requireClassWritable(perspective, userId);
        // 의견 필수 클래스의 의견은 리포트 자료라 삭제 대신 수정만 허용 (PRD §7, 기획 확인 10/23)
        if (perspective.getClassroom() != null && perspective.getClassroom().isCommentRequired()) {
            throw new CustomException(ErrorCode.CLASS_REQUIRED_PERSPECTIVE_UNDELETABLE);
        }

        List<PerspectiveComment> comments = perspectiveCommentRepository.findByPerspective(perspective);
        if (!comments.isEmpty()) {
            commentLikeRepository.deleteAllByCommentIn(comments);
            commentReportRepository.deleteAllByCommentIn(comments);
        }
        perspectiveCommentRepository.deleteAllByPerspective(perspective);

        perspectiveLikeRepository.deleteAllByPerspective(perspective);
        perspectiveReportRepository.deleteAllByPerspective(perspective);
        perspectiveRepository.delete(perspective);
    }

    @Transactional
    public UpdatePerspectiveResponse updatePerspective(Long perspectiveId, Long userId, UpdatePerspectiveRequest request) {
        requireActiveUserForWrite(userId);
        Perspective perspective = findPerspectiveById(perspectiveId);
        validateOwnership(perspective, userId);
        requireClassWritable(perspective, userId);
        perspective.updateContent(request.content());
        perspective.updateStatus(PerspectiveStatus.PUBLISHED);
        return new UpdatePerspectiveResponse(perspective.getId(), perspective.getContent(), perspective.getUpdatedAt());
    }

    private void requireActiveUserForWrite(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    public List<MyPerspectiveResponse> getMyPerspective(Long battleId, Long userId) {
        return getMyPerspective(battleId, userId, null);
    }

    public List<MyPerspectiveResponse> getMyPerspective(Long battleId, Long userId, Classroom classroom) {
        battleService.findById(battleId);
        List<Perspective> perspectives = perspectiveRepository
                .findByBattleIdAndUserIdAndClassroomOrderByCreatedAtDesc(battleId, userId, classroom);

        UserSummary user = userQueryService.findSummaryById(userId);
        String characterImageUrl = resolveCharacterImageUrl(user.characterType());
        String name = classDisplayNameResolver.nameOf(classroom, userId, user.nickname());

        return perspectives.stream()
                .map(perspective -> {
                    BattleOption option = perspective.getOption();
                    boolean isLiked = perspectiveLikeRepository.existsByPerspectiveAndUserId(perspective, userId);
                    return new MyPerspectiveResponse(
                            perspective.getId(),
                            new MyPerspectiveResponse.UserSummary(ClassDisplayNameResolver.userTagOf(classroom, user.userTag()), name, user.characterType(), characterImageUrl),
                            new MyPerspectiveResponse.OptionSummary(option.getId(), BattleOptionDisplay.opinion(option), option.getTitle(), option.getStance()),
                            perspective.getContent(),
                            perspective.getLikeCount(),
                            perspective.getCommentCount(),
                            isLiked,
                            perspective.getStatus(),
                            perspective.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public void retryModeration(Long perspectiveId, Long userId) {
        Perspective perspective = findPerspectiveById(perspectiveId);
        validateOwnership(perspective, userId);
        requireClassWritable(perspective, userId);
        if (perspective.getStatus() != PerspectiveStatus.MODERATION_FAILED) {
            throw new CustomException(ErrorCode.PERSPECTIVE_MODERATION_NOT_FAILED);
        }
        perspective.updateStatus(PerspectiveStatus.PENDING);
        gptModerationService.moderate(perspectiveId, perspective.getContent());
    }

    private Perspective findPerspectiveById(Long perspectiveId) {
        return perspectiveRepository.findById(perspectiveId)
                .orElseThrow(() -> new CustomException(ErrorCode.PERSPECTIVE_NOT_FOUND));
    }

    private void validateOwnership(Perspective perspective, Long userId) {
        if (!perspective.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.PERSPECTIVE_FORBIDDEN);
        }
    }

    /** 클래스 의견이면 운영자/ACTIVE 멤버만 읽을 수 있다. 일반 의견은 그대로 통과 */
    private void requireClassReadable(Perspective perspective, Long userId) {
        if (perspective.getClassroom() != null) {
            classAccessService.requireReadable(perspective.getClassroom(), userId);
        }
    }

    /** 클래스 의견이면 ACTIVE 멤버이고 마감 전일 때만 수정·삭제할 수 있다. 일반 의견은 그대로 통과 */
    private void requireClassWritable(Perspective perspective, Long userId) {
        if (perspective.getClassroom() != null) {
            classAccessService.requireWritable(perspective.getClassroom(), userId, LocalDateTime.now(KST));
        }
    }

    private BattleOption findFilterOption(Battle battle, Long optionId) {
        BattleOption option = battleService.findOptionById(optionId);
        if (option.getBattle() == null || !battle.getId().equals(option.getBattle().getId())) {
            throw new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND);
        }
        return option;
    }

    private String resolveCharacterImageUrl(String characterType) {
        if (characterType == null || characterType.isBlank()) {
            return null;
        }
        return s3PresignedUrlService.generatePresignedUrl(CharacterType.resolveImageKey(characterType));
    }
}
