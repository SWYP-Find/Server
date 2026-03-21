package com.swyp.app.domain.user.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import com.swyp.app.domain.battle.repository.BattleOptionRepository;
import com.swyp.app.domain.battle.repository.BattleRepository;
import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveComment;
import com.swyp.app.domain.perspective.entity.PerspectiveLike;
import com.swyp.app.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.app.domain.perspective.repository.PerspectiveLikeRepository;
import com.swyp.app.domain.user.dto.request.UpdateNotificationSettingsRequest;
import com.swyp.app.domain.user.dto.response.BattleRecordListResponse;
import com.swyp.app.domain.user.dto.response.ContentActivityListResponse;
import com.swyp.app.domain.user.dto.response.MypageResponse;
import com.swyp.app.domain.user.dto.response.NoticeDetailResponse;
import com.swyp.app.domain.user.dto.response.NoticeListResponse;
import com.swyp.app.domain.user.dto.response.NotificationSettingsResponse;
import com.swyp.app.domain.user.dto.response.RecapResponse;
import com.swyp.app.domain.user.entity.ActivityType;
import com.swyp.app.domain.user.entity.Notice;
import com.swyp.app.domain.user.entity.NoticeType;
import com.swyp.app.domain.user.entity.PhilosopherType;
import com.swyp.app.domain.user.entity.TierCode;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.entity.UserProfile;
import com.swyp.app.domain.user.entity.UserSettings;
import com.swyp.app.domain.user.entity.UserTendencyScore;
import com.swyp.app.domain.user.entity.VoteSide;
import com.swyp.app.domain.user.repository.NoticeRepository;
import com.swyp.app.domain.vote.entity.Vote;
import com.swyp.app.domain.vote.repository.VoteRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final UserService userService;
    private final NoticeRepository noticeRepository;
    private final VoteRepository voteRepository;
    private final BattleRepository battleRepository;
    private final BattleOptionRepository battleOptionRepository;
    private final PerspectiveCommentRepository perspectiveCommentRepository;
    private final PerspectiveLikeRepository perspectiveLikeRepository;

    public MypageResponse getMypage() {
        User user = userService.findCurrentUser();
        UserProfile profile = userService.findUserProfile(user.getId());

        MypageResponse.ProfileInfo profileInfo = new MypageResponse.ProfileInfo(
                user.getUserTag(),
                profile.getNickname(),
                profile.getCharacterType(),
                profile.getMannerTemperature()
        );

        // TODO: 철학자 산출 로직 확정 후 구현, 현재는 임시로 SOCRATES 반환
        MypageResponse.PhilosopherInfo philosopherInfo = new MypageResponse.PhilosopherInfo(
                PhilosopherType.SOCRATES
        );

        // TODO: 포인트 계산 - 타 도메인(vote) 연동 필요, 현재는 WANDERER / 0P
        MypageResponse.TierInfo tierInfo = new MypageResponse.TierInfo(
                TierCode.WANDERER,
                TierCode.WANDERER.getLabel(),
                0
        );

        return new MypageResponse(profileInfo, philosopherInfo, tierInfo);
    }

    public RecapResponse getRecap() {
        User user = userService.findCurrentUser();
        UserTendencyScore score = userService.findUserTendencyScore(user.getId());

        // TODO: 철학자 산출 로직 확정 후 구현, 현재는 임시 값 반환
        RecapResponse.PhilosopherCard myCard = new RecapResponse.PhilosopherCard(PhilosopherType.SOCRATES);
        RecapResponse.PhilosopherCard bestMatchCard = new RecapResponse.PhilosopherCard(PhilosopherType.PLATO);
        RecapResponse.PhilosopherCard worstMatchCard = new RecapResponse.PhilosopherCard(PhilosopherType.MARX);

        RecapResponse.Scores scores = new RecapResponse.Scores(
                score.getPrinciple(),
                score.getReason(),
                score.getIndividual(),
                score.getChange(),
                score.getInner(),
                score.getIdeal()
        );

        // TODO: 타 도메인(vote, perspective) 연동 필요, 현재는 0/빈값 반환
        RecapResponse.PreferenceReport preferenceReport = new RecapResponse.PreferenceReport(
                0, 0, 0, Collections.emptyList()
        );

        return new RecapResponse(myCard, bestMatchCard, worstMatchCard, scores, preferenceReport);
    }

    public BattleRecordListResponse getBattleRecords(Integer offset, Integer size, VoteSide voteSide) {
        User user = userService.findCurrentUser();
        int pageOffset = offset == null || offset < 0 ? 0 : offset;
        int pageSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size;
        PageRequest pageable = PageRequest.of(pageOffset / pageSize, pageSize);

        BattleOptionLabel label = voteSide != null ? toOptionLabel(voteSide) : null;

        List<Vote> votes = label != null
                ? voteRepository.findByUserIdAndPreVoteOptionLabelOrderByCreatedAtDesc(user.getId(), label, pageable)
                : voteRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        long totalCount = label != null
                ? voteRepository.countByUserIdAndPreVoteOptionLabel(user.getId(), label)
                : voteRepository.countByUserId(user.getId());

        List<BattleRecordListResponse.BattleRecordItem> items = votes.stream()
                .map(vote -> new BattleRecordListResponse.BattleRecordItem(
                        vote.getBattle().getId().toString(),
                        vote.getId().toString(),
                        toVoteSide(vote.getPreVoteOption().getLabel()),
                        vote.getBattle().getTitle(),
                        vote.getBattle().getSummary(),
                        vote.getCreatedAt()
                ))
                .toList();

        int nextOffset = pageOffset + pageSize;
        boolean hasNext = nextOffset < totalCount;

        return new BattleRecordListResponse(items, hasNext ? nextOffset : null, hasNext);
    }

    public ContentActivityListResponse getContentActivities(Integer offset, Integer size, ActivityType activityType) {
        User user = userService.findCurrentUser();
        int pageOffset = offset == null || offset < 0 ? 0 : offset;
        int pageSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size;
        PageRequest pageable = PageRequest.of(pageOffset / pageSize, pageSize);

        if (activityType == ActivityType.LIKE) {
            return getLikeActivities(user, pageOffset, pageSize, pageable);
        } else if (activityType == ActivityType.COMMENT) {
            return getCommentActivities(user, pageOffset, pageSize, pageable);
        }

        // activityType이 null이면 댓글 기준으로 반환 (기본값)
        return getCommentActivities(user, pageOffset, pageSize, pageable);
    }

    private ContentActivityListResponse getCommentActivities(User user, int pageOffset, int pageSize, PageRequest pageable) {
        List<PerspectiveComment> comments = perspectiveCommentRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        long totalCount = perspectiveCommentRepository.countByUserId(user.getId());

        List<Perspective> perspectives = comments.stream().map(PerspectiveComment::getPerspective).toList();
        Map<UUID, Battle> battleMap = loadBattles(perspectives);
        Map<UUID, BattleOption> optionMap = loadOptions(perspectives);

        List<ContentActivityListResponse.ContentActivityItem> items = comments.stream()
                .map(comment -> {
                    Perspective perspective = comment.getPerspective();
                    Battle battle = battleMap.get(perspective.getBattleId());
                    BattleOption option = optionMap.get(perspective.getOptionId());
                    return toContentActivityItem(
                            comment.getId().toString(),
                            ActivityType.COMMENT,
                            perspective,
                            battle,
                            option,
                            comment.getContent(),
                            comment.getCreatedAt()
                    );
                })
                .toList();

        int nextOffset = pageOffset + pageSize;
        boolean hasNext = nextOffset < totalCount;
        return new ContentActivityListResponse(items, hasNext ? nextOffset : null, hasNext);
    }

    private ContentActivityListResponse getLikeActivities(User user, int pageOffset, int pageSize, PageRequest pageable) {
        List<PerspectiveLike> likes = perspectiveLikeRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        long totalCount = perspectiveLikeRepository.countByUserId(user.getId());

        List<Perspective> perspectives = likes.stream().map(PerspectiveLike::getPerspective).toList();
        Map<UUID, Battle> battleMap = loadBattles(perspectives);
        Map<UUID, BattleOption> optionMap = loadOptions(perspectives);

        List<ContentActivityListResponse.ContentActivityItem> items = likes.stream()
                .map(like -> {
                    Perspective perspective = like.getPerspective();
                    Battle battle = battleMap.get(perspective.getBattleId());
                    BattleOption option = optionMap.get(perspective.getOptionId());
                    return toContentActivityItem(
                            like.getId().toString(),
                            ActivityType.LIKE,
                            perspective,
                            battle,
                            option,
                            perspective.getContent(),
                            like.getCreatedAt()
                    );
                })
                .toList();

        int nextOffset = pageOffset + pageSize;
        boolean hasNext = nextOffset < totalCount;
        return new ContentActivityListResponse(items, hasNext ? nextOffset : null, hasNext);
    }

    private ContentActivityListResponse.ContentActivityItem toContentActivityItem(
            String activityId, ActivityType activityType, Perspective perspective,
            Battle battle, BattleOption option,
            String content, java.time.LocalDateTime createdAt) {

        com.swyp.app.domain.user.dto.response.UserSummary author = userService.findSummaryById(perspective.getUserId());
        ContentActivityListResponse.AuthorInfo authorInfo = new ContentActivityListResponse.AuthorInfo(
                author.userTag(), author.nickname(), com.swyp.app.domain.user.entity.CharacterType.from(author.characterType())
        );

        return new ContentActivityListResponse.ContentActivityItem(
                activityId,
                activityType,
                perspective.getId().toString(),
                perspective.getBattleId().toString(),
                battle != null ? battle.getTitle() : null,
                authorInfo,
                option != null ? option.getStance() : null,
                content,
                perspective.getLikeCount(),
                createdAt
        );
    }

    private Map<UUID, Battle> loadBattles(List<Perspective> perspectives) {
        List<UUID> battleIds = perspectives.stream()
                .map(Perspective::getBattleId)
                .distinct()
                .toList();
        return battleRepository.findAllById(battleIds).stream()
                .collect(Collectors.toMap(Battle::getId, Function.identity()));
    }

    private Map<UUID, BattleOption> loadOptions(List<Perspective> perspectives) {
        List<UUID> optionIds = perspectives.stream()
                .map(Perspective::getOptionId)
                .distinct()
                .toList();
        return battleOptionRepository.findAllById(optionIds).stream()
                .collect(Collectors.toMap(BattleOption::getId, Function.identity()));
    }

    public NotificationSettingsResponse getNotificationSettings() {
        User user = userService.findCurrentUser();
        UserSettings settings = userService.findUserSettings(user.getId());
        return toNotificationSettingsResponse(settings);
    }

    @Transactional
    public NotificationSettingsResponse updateNotificationSettings(UpdateNotificationSettingsRequest request) {
        User user = userService.findCurrentUser();
        UserSettings settings = userService.findUserSettings(user.getId());
        settings.update(
                request.newBattleEnabled(),
                request.battleResultEnabled(),
                request.commentReplyEnabled(),
                request.newCommentEnabled(),
                request.contentLikeEnabled(),
                request.marketingEventEnabled()
        );
        return toNotificationSettingsResponse(settings);
    }

    public NoticeListResponse getNotices(NoticeType type) {
        List<Notice> notices = type == null
                ? noticeRepository.findAllByOrderByIsPinnedDescPublishedAtDesc()
                : noticeRepository.findByTypeOrderByIsPinnedDescPublishedAtDesc(type);

        List<NoticeListResponse.NoticeItem> items = notices.stream()
                .map(notice -> new NoticeListResponse.NoticeItem(
                        notice.getId(),
                        notice.getType(),
                        notice.getTitle(),
                        notice.getBodyPreview(),
                        notice.isPinned(),
                        notice.getPublishedAt()
                ))
                .toList();

        return new NoticeListResponse(items);
    }

    public NoticeDetailResponse getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_INVALID_PARAMETER));

        return new NoticeDetailResponse(
                notice.getId(),
                notice.getType(),
                notice.getTitle(),
                notice.getBody(),
                notice.isPinned(),
                notice.getPublishedAt()
        );
    }

    private VoteSide toVoteSide(BattleOptionLabel label) {
        return label == BattleOptionLabel.A ? VoteSide.PRO : VoteSide.CON;
    }

    private BattleOptionLabel toOptionLabel(VoteSide voteSide) {
        return voteSide == VoteSide.PRO ? BattleOptionLabel.A : BattleOptionLabel.B;
    }

    private NotificationSettingsResponse toNotificationSettingsResponse(UserSettings settings) {
        return new NotificationSettingsResponse(
                settings.isNewBattleEnabled(),
                settings.isBattleResultEnabled(),
                settings.isCommentReplyEnabled(),
                settings.isNewCommentEnabled(),
                settings.isContentLikeEnabled(),
                settings.isMarketingEventEnabled()
        );
    }
}
