package com.swyp.app.domain.user.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import com.swyp.app.domain.battle.service.BattleQueryService;
import com.swyp.app.domain.perspective.entity.Perspective;
import com.swyp.app.domain.perspective.entity.PerspectiveComment;
import com.swyp.app.domain.perspective.entity.PerspectiveLike;
import com.swyp.app.domain.perspective.service.PerspectiveQueryService;
import com.swyp.app.domain.user.dto.request.UpdateNotificationSettingsRequest;
import com.swyp.app.domain.user.dto.response.BattleRecordListResponse;
import com.swyp.app.domain.user.dto.response.ContentActivityListResponse;
import com.swyp.app.domain.user.dto.response.MypageResponse;
import com.swyp.app.domain.notice.dto.response.NoticeSummaryResponse;
import com.swyp.app.domain.notice.enums.NoticePlacement;
import com.swyp.app.domain.notice.enums.NoticeType;
import com.swyp.app.domain.notice.service.NoticeService;
import com.swyp.app.domain.user.dto.response.NoticeDetailResponse;
import com.swyp.app.domain.user.dto.response.NoticeListResponse;
import com.swyp.app.domain.user.dto.response.NotificationSettingsResponse;
import com.swyp.app.domain.user.dto.response.RecapResponse;
import com.swyp.app.domain.user.dto.response.UserSummary;
import com.swyp.app.domain.user.entity.ActivityType;
import com.swyp.app.domain.user.entity.CharacterType;
import com.swyp.app.domain.user.entity.PhilosopherType;
import com.swyp.app.domain.user.entity.TierCode;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.entity.UserProfile;
import com.swyp.app.domain.user.entity.UserSettings;
import com.swyp.app.domain.user.entity.UserTendencyScore;
import com.swyp.app.domain.user.entity.VoteSide;
import com.swyp.app.domain.vote.entity.Vote;
import com.swyp.app.domain.vote.service.VoteQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final UserService userService;
    private final NoticeService noticeService;
    private final CreditService creditService;
    private final VoteQueryService voteQueryService;
    private final BattleQueryService battleQueryService;
    private final PerspectiveQueryService perspectiveQueryService;

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

        int currentPoint = creditService.getTotalPoints(user.getId());
        TierCode tierCode = TierCode.fromPoints(currentPoint);
        MypageResponse.TierInfo tierInfo = new MypageResponse.TierInfo(
                tierCode,
                tierCode.getLabel(),
                currentPoint
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

        RecapResponse.PreferenceReport preferenceReport = buildPreferenceReport(user.getId());

        return new RecapResponse(myCard, bestMatchCard, worstMatchCard, scores, preferenceReport);
    }

    private RecapResponse.PreferenceReport buildPreferenceReport(Long userId) {
        long totalParticipation = voteQueryService.countTotalParticipation(userId);
        long opinionChanges = voteQueryService.countOpinionChanges(userId);
        int battleWinRate = voteQueryService.calculateBattleWinRate(userId);

        List<Long> battleIds = voteQueryService.findParticipatedBattleIds(userId);
        Map<String, Long> topTags = battleQueryService.getTopTagsByBattleIds(battleIds, 4);

        List<RecapResponse.FavoriteTopic> favoriteTopics = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<String, Long> entry : topTags.entrySet()) {
            favoriteTopics.add(new RecapResponse.FavoriteTopic(rank++, entry.getKey(), entry.getValue().intValue()));
        }

        return new RecapResponse.PreferenceReport(
                (int) totalParticipation,
                (int) opinionChanges,
                battleWinRate,
                favoriteTopics
        );
    }

    public BattleRecordListResponse getBattleRecords(Integer offset, Integer size, VoteSide voteSide) {
        User user = userService.findCurrentUser();
        int pageOffset = offset == null || offset < 0 ? 0 : offset;
        int pageSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size;

        BattleOptionLabel label = voteSide != null ? toOptionLabel(voteSide) : null;

        List<Vote> votes = voteQueryService.findUserVotes(user.getId(), pageOffset, pageSize, label);
        long totalCount = voteQueryService.countUserVotes(user.getId(), label);

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

        if (activityType == ActivityType.LIKE) {
            return buildLikeActivities(user, pageOffset, pageSize);
        }
        return buildCommentActivities(user, pageOffset, pageSize);
    }

    private ContentActivityListResponse buildCommentActivities(User user, int pageOffset, int pageSize) {
        List<PerspectiveComment> comments = perspectiveQueryService.findUserComments(user.getId(), pageOffset, pageSize);
        long totalCount = perspectiveQueryService.countUserComments(user.getId());

        List<Perspective> perspectives = comments.stream().map(PerspectiveComment::getPerspective).toList();
        Map<Long, Battle> battleMap = loadBattles(perspectives);
        Map<Long, BattleOption> optionMap = loadOptions(perspectives);

        List<ContentActivityListResponse.ContentActivityItem> items = comments.stream()
                .map(comment -> {
                    Perspective p = comment.getPerspective();
                    return toActivityItem(comment.getId().toString(), ActivityType.COMMENT, p,
                            battleMap.get(p.getBattleId()), optionMap.get(p.getOptionId()),
                            comment.getContent(), comment.getCreatedAt());
                })
                .toList();

        int nextOffset = pageOffset + pageSize;
        boolean hasNext = nextOffset < totalCount;
        return new ContentActivityListResponse(items, hasNext ? nextOffset : null, hasNext);
    }

    private ContentActivityListResponse buildLikeActivities(User user, int pageOffset, int pageSize) {
        List<PerspectiveLike> likes = perspectiveQueryService.findUserLikes(user.getId(), pageOffset, pageSize);
        long totalCount = perspectiveQueryService.countUserLikes(user.getId());

        List<Perspective> perspectives = likes.stream().map(PerspectiveLike::getPerspective).toList();
        Map<Long, Battle> battleMap = loadBattles(perspectives);
        Map<Long, BattleOption> optionMap = loadOptions(perspectives);

        List<ContentActivityListResponse.ContentActivityItem> items = likes.stream()
                .map(like -> {
                    Perspective p = like.getPerspective();
                    return toActivityItem(like.getId().toString(), ActivityType.LIKE, p,
                            battleMap.get(p.getBattleId()), optionMap.get(p.getOptionId()),
                            p.getContent(), like.getCreatedAt());
                })
                .toList();

        int nextOffset = pageOffset + pageSize;
        boolean hasNext = nextOffset < totalCount;
        return new ContentActivityListResponse(items, hasNext ? nextOffset : null, hasNext);
    }

    private ContentActivityListResponse.ContentActivityItem toActivityItem(
            String activityId, ActivityType activityType, Perspective perspective,
            Battle battle, BattleOption option, String content, LocalDateTime createdAt) {

        UserSummary author = userService.findSummaryById(perspective.getUserId());
        ContentActivityListResponse.AuthorInfo authorInfo = new ContentActivityListResponse.AuthorInfo(
                author.userTag(), author.nickname(), CharacterType.from(author.characterType())
        );

        return new ContentActivityListResponse.ContentActivityItem(
                activityId, activityType,
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

    private Map<Long, Battle> loadBattles(List<Perspective> perspectives) {
        List<Long> battleIds = perspectives.stream().map(Perspective::getBattleId).distinct().toList();
        return battleQueryService.findBattlesByIds(battleIds);
    }

    private Map<Long, BattleOption> loadOptions(List<Perspective> perspectives) {
        List<Long> optionIds = perspectives.stream().map(Perspective::getOptionId).distinct().toList();
        return battleQueryService.findOptionsByIds(optionIds);
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
        List<NoticeSummaryResponse> notices = noticeService.getActiveNotices(
                NoticePlacement.NOTICE_BOARD, type, null
        );

        List<NoticeListResponse.NoticeItem> items = notices.stream()
                .map(notice -> new NoticeListResponse.NoticeItem(
                        notice.noticeId(), notice.type(), notice.title(),
                        notice.body(), notice.pinned(), notice.startsAt()
                ))
                .toList();

        return new NoticeListResponse(items);
    }

    public NoticeDetailResponse getNoticeDetail(Long noticeId) {
        com.swyp.app.domain.notice.dto.response.NoticeDetailResponse notice =
                noticeService.getNoticeDetail(noticeId);
        return new NoticeDetailResponse(
                notice.noticeId(), notice.type(), notice.title(),
                notice.body(), notice.pinned(), notice.startsAt()
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
                settings.isNewBattleEnabled(), settings.isBattleResultEnabled(),
                settings.isCommentReplyEnabled(), settings.isNewCommentEnabled(),
                settings.isContentLikeEnabled(), settings.isMarketingEventEnabled()
        );
    }
}
