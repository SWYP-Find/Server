package com.swyp.picke.domain.user.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.service.BattleQueryService;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.perspective.entity.PerspectiveLike;
import com.swyp.picke.domain.perspective.service.PerspectiveQueryService;
import com.swyp.picke.domain.user.dto.request.UpdateNotificationSettingsRequest;
import com.swyp.picke.domain.user.dto.response.*;
import com.swyp.picke.domain.user.enums.ActivityType;
import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import com.swyp.picke.domain.user.enums.TierCode;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserProfile;
import com.swyp.picke.domain.user.entity.UserSettings;
import com.swyp.picke.domain.user.enums.VoteSide;
import com.swyp.picke.domain.vote.entity.Vote;
import com.swyp.picke.domain.vote.service.VoteQueryService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
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
    private final CreditService creditService;
    private final VoteQueryService voteQueryService;
    private final BattleQueryService battleQueryService;
    private final PerspectiveQueryService perspectiveQueryService;
    private final S3PresignedUrlService s3PresignedUrlService;

    @Transactional
    public MypageResponse getMypage() {
        User user = userService.findCurrentUser();
        UserProfile profile = userService.findUserProfile(user.getId());

        CharacterType characterType = profile.getCharacterType();
        String characterImageUrl = resolveCharacterImageUrl(characterType);

        MypageResponse.ProfileInfo profileInfo = new MypageResponse.ProfileInfo(
                user.getUserTag(),
                profile.getNickname(),
                characterType,
                characterType != null ? characterType.getLabel() : null,
                characterImageUrl,
                profile.getMannerTemperature()
        );

        PhilosopherType philosopherType = resolvePhilosopherType(user.getId(), profile);
        MypageResponse.PhilosopherInfo philosopherInfo = philosopherType != null
                ? new MypageResponse.PhilosopherInfo(
                        philosopherType,
                        philosopherType.getLabel(),
                        philosopherType.getTypeName(),
                        philosopherType.getDescription(),
                        s3PresignedUrlService.generatePresignedUrl(
                                PhilosopherType.resolveImageKey(philosopherType.getLabel())
                        ))
                : null;

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
        UserProfile profile = userService.findUserProfile(user.getId());

        PhilosopherType philosopherType = profile.getPhilosopherType();
        if (philosopherType == null) {
            return null;
        }

        RecapResponse.PhilosopherCard myCard = toPhilosopherCard(philosopherType);
        RecapResponse.PhilosopherCard bestMatchCard = toPhilosopherCard(philosopherType.getBestMatch());
        RecapResponse.PhilosopherCard worstMatchCard = toPhilosopherCard(philosopherType.getWorstMatch());

        RecapResponse.Scores scores = new RecapResponse.Scores(
                philosopherType.getPrinciple(),
                philosopherType.getReason(),
                philosopherType.getIndividual(),
                philosopherType.getChange(),
                philosopherType.getInner(),
                philosopherType.getIdeal()
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

        List<Long> battleIds = votes.stream().map(v -> v.getBattle().getId()).toList();
        Map<Long, String> categoryMap = battleQueryService.getCategoryNamesByBattleIds(battleIds); // 추가 필요

        List<BattleRecordListResponse.BattleRecordItem> items = votes.stream()
                .map(vote -> {
                    Battle battle = vote.getBattle();
                    BattleOption selectedOption = vote.getPostVoteOption() != null
                            ? vote.getPostVoteOption() : vote.getPreVoteOption();
                    VoteSide side = selectedOption.getLabel() == BattleOptionLabel.A
                            ? VoteSide.PRO : VoteSide.CON;
                    String category = categoryMap.get(battle.getId());

                    return new BattleRecordListResponse.BattleRecordItem(
                            battle.getId().toString(),
                            vote.getId().toString(),
                            side,
                            category,
                            battle.getTitle(),
                            battle.getSummary(),
                            vote.getCreatedAt()
                    );
                })
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

        UserProfile profile = userService.findUserProfile(user.getId());
        String myCharacterImageUrl = resolveCharacterImageUrl(profile.getCharacterType());

        List<Perspective> perspectives = comments.stream().map(PerspectiveComment::getPerspective).toList();
        Map<Long, Battle> battleMap = loadBattles(perspectives);
        Map<Long, BattleOption> optionMap = loadOptions(perspectives);

        List<ContentActivityListResponse.ContentActivityItem> items = comments.stream()
                .map(comment -> {
                    Perspective p = comment.getPerspective();
                    return toActivityItem(comment.getId().toString(), ActivityType.COMMENT, p,
                            battleMap.get(p.getBattle().getId()), optionMap.get(p.getOption().getId()),
                            comment.getContent(), comment.getCreatedAt(), myCharacterImageUrl);
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
                    UserSummary perspectiveAuthor = userService.findSummaryById(p.getUser().getId());
                    String authorCharacterImageUrl = resolveCharacterImageUrl(perspectiveAuthor.characterType());
                    return toActivityItem(like.getId().toString(), ActivityType.LIKE, p,
                            battleMap.get(p.getBattle().getId()), optionMap.get(p.getOption().getId()),
                            p.getContent(), like.getCreatedAt(), authorCharacterImageUrl);
                })
                .toList();

        int nextOffset = pageOffset + pageSize;
        boolean hasNext = nextOffset < totalCount;
        return new ContentActivityListResponse(items, hasNext ? nextOffset : null, hasNext);
    }

    private ContentActivityListResponse.ContentActivityItem toActivityItem(
            String activityId, ActivityType activityType, Perspective perspective,
            Battle battle, BattleOption option, String content, LocalDateTime createdAt,
            String characterImageUrl) {

        UserSummary author = userService.findSummaryById(perspective.getUser().getId());
        ContentActivityListResponse.AuthorInfo authorInfo = new ContentActivityListResponse.AuthorInfo(
                author.userTag(),
                author.nickname(),
                author.characterType() != null ? CharacterType.from(author.characterType()) : null,
                characterImageUrl
        );

        VoteSide voteSide = option != null
                ? (option.getLabel() == BattleOptionLabel.A ? VoteSide.PRO : VoteSide.CON)
                : null;

        return new ContentActivityListResponse.ContentActivityItem(
                activityId, activityType,
                perspective.getId().toString(),
                perspective.getBattle().getId().toString(),
                battle != null ? battle.getTitle() : null,
                authorInfo,
                voteSide,
                content,
                perspective.getLikeCount(),
                createdAt
        );
    }

    private Map<Long, Battle> loadBattles(List<Perspective> perspectives) {
        List<Long> battleIds = perspectives.stream().map(p -> p.getBattle().getId()).distinct().toList();
        return battleQueryService.findBattlesByIds(battleIds);
    }

    private Map<Long, BattleOption> loadOptions(List<Perspective> perspectives) {
        List<Long> optionIds = perspectives.stream().map(p -> p.getOption().getId()).distinct().toList();
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

    private static final int PHILOSOPHER_CALC_THRESHOLD = 5;

    private PhilosopherType resolvePhilosopherType(Long userId, UserProfile profile) {
        if (profile.getPhilosopherType() != null) {
            return profile.getPhilosopherType();
        }

        long totalVotes = voteQueryService.countTotalParticipation(userId);
        if (totalVotes < PHILOSOPHER_CALC_THRESHOLD) {
            return null;
        }

        List<Long> optionIds = voteQueryService.findFirstNVotedOptionIds(userId, PHILOSOPHER_CALC_THRESHOLD);

        return battleQueryService.getTopPhilosopherTagNameFromOptions(optionIds)
                .map(PhilosopherType::fromLabel)
                .map(type -> {
                    profile.updatePhilosopherType(type);
                    return type;
                })
                .orElseThrow(() -> new CustomException(ErrorCode.PHILOSOPHER_CALC_FAILED));
    }

    private RecapResponse.PhilosopherCard toPhilosopherCard(PhilosopherType type) {
        return new RecapResponse.PhilosopherCard(
                type,
                type.getLabel(),
                type.getTypeName(),
                type.getDescription(),
                type.getKeywordTags(),
                s3PresignedUrlService.generatePresignedUrl(
                        PhilosopherType.resolveImageKey(type.getLabel())
                )
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

    private String resolveCharacterImageUrl(CharacterType characterType) {
        String imageKey = CharacterType.resolveImageKey(characterType);
        return imageKey != null ? s3PresignedUrlService.generatePresignedUrl(imageKey) : null;
    }

    private String resolveCharacterImageUrl(String characterType) {
        if (characterType == null || characterType.isBlank()) {
            return null;
        }
        String imageKey = CharacterType.resolveImageKey(characterType);
        return s3PresignedUrlService.generatePresignedUrl(imageKey);
    }
}
