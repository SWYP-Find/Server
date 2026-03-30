package com.swyp.picke.domain.battle.converter;

import com.swyp.picke.domain.battle.dto.request.AdminBattleCreateRequest;
import com.swyp.picke.domain.battle.dto.response.*;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.entity.BattleOptionTag;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.util.ResourceUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BattleConverter {

    private final BattleOptionTagRepository optionTagRepository;
    private final ResourceUrlProvider urlProvider;
    private static final String BASE_SHARE_URL = "https://pique.app/battles/";

    public Battle toEntity(AdminBattleCreateRequest request, User admin) {
        return Battle.builder()
                .title(request.title())
                .titlePrefix(request.titlePrefix())
                .titleSuffix(request.titleSuffix())
                .itemA(request.itemA())
                .itemADesc(request.itemADesc())
                .itemB(request.itemB())
                .itemBDesc(request.itemBDesc())
                .summary(request.summary())
                .description(request.description())
                .thumbnailUrl(request.thumbnailUrl())
                .type(request.type())
                .targetDate(request.targetDate())
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
                battle.getType(),
                battle.getViewCount() == null ? 0 : battle.getViewCount(),
                battle.getTotalParticipantsCount() == null ? 0L : battle.getTotalParticipantsCount(),
                battle.getAudioDuration() == null ? 0 : battle.getAudioDuration(),
                toTagResponses(tags, null),
                toTodayOptionResponses(options),
                battle.getTitlePrefix(),
                battle.getTitleSuffix(),
                battle.getItemA(),
                battle.getItemADesc(),
                battle.getItemB(),
                battle.getItemBDesc()
        );
    }

    public BattleSimpleResponse toSimpleResponse(Battle battle) {
        return new BattleSimpleResponse(
                battle.getId(),
                battle.getTitle(),
                urlProvider.getImageUrl(FileCategory.BATTLE, battle.getThumbnailUrl()),
                battle.getType() != null ? battle.getType().name() : "BATTLE",
                battle.getStatus() != null ? battle.getStatus().name() : "PENDING",
                battle.getCreatedAt()
        );
    }

    public AdminBattleDetailResponse toAdminDetailResponse(Battle battle, List<Tag> tags, List<BattleOption> options) {
        return new AdminBattleDetailResponse(
                battle.getId(),
                battle.getTitle(),
                battle.getTitlePrefix(),
                battle.getTitleSuffix(),
                battle.getSummary(),
                battle.getDescription(),
                urlProvider.getImageUrl(FileCategory.BATTLE, battle.getThumbnailUrl()),
                battle.getType(),
                battle.getItemA(),
                battle.getItemADesc(),
                battle.getItemB(),
                battle.getItemBDesc(),
                battle.getTargetDate(),
                battle.getStatus(),
                battle.getCreatorType(),
                toTagResponses(tags, null),
                toOptionResponses(options),
                battle.getCreatedAt(),
                battle.getUpdatedAt()
        );
    }

    public BattleUserDetailResponse toUserDetailResponse(
            Battle battle, List<Tag> tags, List<BattleOption> options,
            Long participantsCount, String voteStatus, UserBattleStep currentStep) {

        BattleSummaryResponse summary = new BattleSummaryResponse(
                battle.getId(),
                battle.getTitle(),
                battle.getSummary(),
                urlProvider.getImageUrl(FileCategory.BATTLE, battle.getThumbnailUrl()),
                battle.getType(),
                battle.getViewCount() == null ? 0 : battle.getViewCount(),
                participantsCount == null ? 0L : participantsCount,
                battle.getAudioDuration() == null ? 0 : battle.getAudioDuration(),
                toTagResponses(tags, null),
                toOptionResponses(options)
        );

        return new BattleUserDetailResponse(
                summary,
                battle.getTitlePrefix(),
                battle.getTitleSuffix(),
                battle.getItemA(),
                battle.getItemADesc(),
                battle.getItemB(),
                battle.getItemBDesc(),
                battle.getDescription(),
                BASE_SHARE_URL + battle.getId(),
                voteStatus,
                currentStep,
                toTagResponses(tags, TagType.CATEGORY),
                toTagResponses(tags, TagType.PHILOSOPHER),
                toTagResponses(tags, TagType.VALUE)
        );
    }

    private List<BattleOptionResponse> toOptionResponses(List<BattleOption> options) {
        if (options == null) return List.of();
        return options.stream()
                .map(option -> {
                    List<Tag> optionTags = optionTagRepository.findByBattleOption(option).stream()
                            .map(BattleOptionTag::getTag)
                            .toList();
                    return new BattleOptionResponse(
                            option.getId(),
                            option.getLabel(),
                            option.getTitle(),
                            option.getStance(),
                            option.getRepresentative(),
                            option.getQuote(),
                            urlProvider.getImageUrl(FileCategory.PHILOSOPHER, option.getImageUrl()),
                            toTagResponses(optionTags, null)
                    );
                }).toList();
    }

    private List<TodayOptionResponse> toTodayOptionResponses(List<BattleOption> options) {
        if (options == null) return List.of();
        return options.stream().map(option -> new TodayOptionResponse(
                option.getId(), option.getLabel(), option.getTitle(),
                option.getRepresentative(), option.getStance(),
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