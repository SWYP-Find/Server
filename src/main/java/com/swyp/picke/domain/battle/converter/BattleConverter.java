package com.swyp.picke.domain.battle.converter;

import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleCreateRequest;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleDetailResponse;
import com.swyp.picke.domain.battle.dto.response.*;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.enums.VoteSide;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.util.ResourceUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BattleConverter {

    private final ResourceUrlProvider urlProvider;
    private static final String BASE_SHARE_URL = "https://pique.app/battles/";
    private static final Comparator<BattleOption> OPTION_SORTER =
            Comparator.comparing((BattleOption option) -> option.getDisplayOrder() == null ? Integer.MAX_VALUE : option.getDisplayOrder())
                    .thenComparing(option -> option.getLabel() == null ? "" : option.getLabel().name())
                    .thenComparing(BattleOption::getId);

    public Battle toEntity(AdminBattleCreateRequest request, User admin) {
        return Battle.builder()
                .title(request.title())
                .summary(request.summary())
                .description(request.description())
                .thumbnailUrl(request.thumbnailUrl())
                .status(request.status())
                .creatorType(BattleCreatorType.ADMIN)
                .creator(admin)
                .build();
    }

    public TodayBattleResponse toTodayResponse(Battle battle, List<Tag> tags, List<BattleOption> options) {
        return new TodayBattleResponse(
                battle.getId(),
                battle.getTitle(),
                battle.getSummary(),
                urlProvider.getImageUrl(FileCategory.BATTLE, battle.getThumbnailUrl()),
                battle.getViewCount() == null ? 0 : battle.getViewCount(),
                battle.getTotalParticipantsCount() == null ? 0L : battle.getTotalParticipantsCount(),
                battle.getAudioDuration() == null ? 0 : battle.getAudioDuration(),
                toTagResponses(tags, null),
                toTodayOptionResponses(options)
        );
    }

    public BattleSimpleResponse toSimpleResponse(Battle battle) {
        return new BattleSimpleResponse(
                battle.getId(),
                battle.getTitle(),
                urlProvider.getImageUrl(FileCategory.BATTLE, battle.getThumbnailUrl()),
                battle.getStatus() != null ? battle.getStatus().name() : "PENDING",
                battle.getCreatedAt()
        );
    }

    public AdminBattleDetailResponse toAdminDetailResponse(Battle battle, List<Tag> tags, List<BattleOption> options, Map<Long, List<Tag>> optionTagsMap) {
        return new AdminBattleDetailResponse(
                battle.getId(),
                battle.getTitle(),
                battle.getSummary(),
                battle.getDescription(),
                urlProvider.getImageUrl(FileCategory.BATTLE, battle.getThumbnailUrl()),
                battle.getAudioDuration(),
                battle.getTargetDate(),
                battle.getStatus(),
                battle.getCreatorType(),
                toTagResponses(tags, null),
                toOptionResponses(options, optionTagsMap),
                battle.getCreatedAt(),
                battle.getUpdatedAt()
        );
    }

    public BattleUserDetailResponse toUserDetailResponse(
            Battle battle, List<Tag> tags, List<BattleOption> options, Map<Long, List<Tag>> optionTagsMap,
            Long participantsCount, VoteSide userVoteStatus, UserBattleStep currentStep) {

        BattleSummaryResponse summary = new BattleSummaryResponse(
                battle.getId(),
                battle.getTitle(),
                battle.getSummary(),
                urlProvider.getImageUrl(FileCategory.BATTLE, battle.getThumbnailUrl()),
                battle.getViewCount() == null ? 0 : battle.getViewCount(),
                participantsCount == null ? 0L : participantsCount,
                battle.getAudioDuration() == null ? 0 : battle.getAudioDuration(),
                toTagResponses(tags, null),
                toOptionResponses(options, optionTagsMap)
        );

        return new BattleUserDetailResponse(
                summary,
                battle.getDescription(),
                BASE_SHARE_URL + battle.getId(),
                userVoteStatus,
                currentStep,
                toTagResponses(tags, TagType.CATEGORY),
                toTagResponses(tags, TagType.PHILOSOPHER),
                toTagResponses(tags, TagType.VALUE)
        );
    }

    public BattleScenarioResponse toScenarioResponse(Battle battle, List<BattleOption> options) {
        List<BattleScenarioResponse.PhilosopherProfileResponse> profiles = options.stream()
                .map(opt -> new BattleScenarioResponse.PhilosopherProfileResponse(
                        opt.getLabel().name(),
                        opt.getRepresentative(),
                        opt.getStance(),
                        urlProvider.getImageUrl(FileCategory.PHILOSOPHER, opt.getImageUrl())
                )).toList();

        return new BattleScenarioResponse(battle.getTitle(), profiles);
    }

    private List<BattleOptionResponse> toOptionResponses(List<BattleOption> options, Map<Long, List<Tag>> optionTagsMap) {
        if (options == null) return List.of();
        return options.stream()
                .sorted(OPTION_SORTER)
                .map(option -> {
                    List<Tag> optionTags = optionTagsMap.getOrDefault(option.getId(), List.of());
                    return new BattleOptionResponse(
                            option.getId(),
                            option.getLabel(),
                            option.getTitle(),
                            option.getStance(),
                            option.getRepresentative(),
                            urlProvider.getImageUrl(FileCategory.PHILOSOPHER, option.getImageUrl()),
                            toTagResponses(optionTags, null)
                    );
                }).toList();
    }

    private List<TodayOptionResponse> toTodayOptionResponses(List<BattleOption> options) {
        if (options == null) return List.of();
        return options.stream()
                .sorted(OPTION_SORTER)
                .map(option -> new TodayOptionResponse(
                        option.getId(),
                        option.getLabel(),
                        option.getTitle(),
                        option.getRepresentative(),
                        option.getStance(),
                        urlProvider.getImageUrl(FileCategory.PHILOSOPHER, option.getImageUrl())
                )).toList();
    }

    private List<BattleTagResponse> toTagResponses(List<Tag> tags, TagType targetType) {
        if (tags == null) return List.of();
        return tags.stream()
                .filter(tag -> targetType == null || tag.getType() == targetType)
                .map(tag -> new BattleTagResponse(tag.getId(), tag.getName(), tag.getType()))
                .toList();
    }
}
