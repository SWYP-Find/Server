package com.swyp.app.domain.battle.converter;

import com.swyp.app.domain.battle.dto.request.AdminBattleCreateRequest;
import com.swyp.app.domain.battle.dto.response.*;
import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.entity.BattleOptionTag;
import com.swyp.app.domain.battle.enums.BattleCreatorType;
import com.swyp.app.domain.battle.enums.BattleStatus;
import com.swyp.app.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.app.domain.tag.entity.Tag;
import com.swyp.app.domain.tag.enums.TagType;
import com.swyp.app.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BattleConverter {

    private final BattleOptionTagRepository optionTagRepository;
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
                .status(BattleStatus.PENDING)
                .creatorType(BattleCreatorType.ADMIN)
                .creator(admin)
                .build();
    }

    public TodayBattleResponse toTodayResponse(Battle b, List<Tag> tags, List<BattleOption> opts) {
        return new TodayBattleResponse(
                b.getId(),
                b.getTitle(),
                b.getSummary(),
                b.getThumbnailUrl(),
                b.getType(),
                b.getViewCount() == null ? 0 : b.getViewCount(),
                b.getTotalParticipantsCount() == null ? 0L : b.getTotalParticipantsCount(),
                b.getAudioDuration() == null ? 0 : b.getAudioDuration(),
                toTagResponses(tags, null),
                toTodayOptionResponses(opts),
                b.getTitlePrefix(),
                b.getTitleSuffix(),
                b.getItemA(),
                b.getItemADesc(),
                b.getItemB(),
                b.getItemBDesc()
        );
    }

    public BattleSimpleResponse toSimpleResponse(Battle b) {
        return new BattleSimpleResponse(
                b.getId(),
                b.getTitle(),
                b.getType() != null ? b.getType().name() : "BATTLE",
                b.getStatus() != null ? b.getStatus().name() : "DRAFT",
                b.getCreatedAt()
        );
    }

    public AdminBattleDetailResponse toAdminDetailResponse(Battle b, List<Tag> tags, List<BattleOption> opts) {
        return new AdminBattleDetailResponse(
                b.getId(),
                b.getTitle(),
                b.getTitlePrefix(),
                b.getTitleSuffix(),
                b.getSummary(),
                b.getDescription(),
                b.getThumbnailUrl(),
                b.getType(),
                b.getItemA(),
                b.getItemADesc(),
                b.getItemB(),
                b.getItemBDesc(),
                b.getTargetDate(),
                b.getStatus(),
                b.getCreatorType(),
                toTagResponses(tags, null),
                toOptionResponses(opts),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }

    public BattleUserDetailResponse toUserDetailResponse(Battle b, List<Tag> tags, List<BattleOption> opts, Long partCount, String voteStatus) {
        BattleSummaryResponse summary = new BattleSummaryResponse(
                b.getId(), b.getTitle(), b.getSummary(), b.getThumbnailUrl(), b.getType(),
                b.getViewCount() == null ? 0 : b.getViewCount(),
                partCount == null ? 0L : partCount,
                b.getAudioDuration() == null ? 0 : b.getAudioDuration(),
                toTagResponses(tags, null),
                toOptionResponses(opts)
        );

        return new BattleUserDetailResponse(
                summary,
                b.getTitlePrefix(),
                b.getTitleSuffix(),
                b.getItemA(),
                b.getItemADesc(),
                b.getItemB(),
                b.getItemBDesc(),
                b.getDescription(),
                BASE_SHARE_URL + b.getId(),
                voteStatus,
                toTagResponses(tags, TagType.CATEGORY),
                toTagResponses(tags, TagType.PHILOSOPHER),
                toTagResponses(tags, TagType.VALUE)
        );
    }

    private List<BattleOptionResponse> toOptionResponses(List<BattleOption> options) {
        if (options == null) return List.of();
        return options.stream()
                .map(o -> {
                    List<Tag> optionTags = optionTagRepository.findByBattleOption(o).stream()
                            .map(BattleOptionTag::getTag)
                            .toList();

                    return new BattleOptionResponse(
                            o.getId(), o.getLabel(), o.getTitle(), o.getStance(),
                            o.getRepresentative(), o.getQuote(), o.getImageUrl(),
                            toTagResponses(optionTags, null)
                    );
                }).toList();
    }

    private List<TodayOptionResponse> toTodayOptionResponses(List<BattleOption> options) {
        if (options == null) return List.of();
        return options.stream().map(o -> new TodayOptionResponse(
                o.getId(), o.getLabel(), o.getTitle(), o.getRepresentative(), o.getStance(), o.getImageUrl()
        )).toList();
    }

    private List<BattleTagResponse> toTagResponses(List<Tag> tags, TagType targetType) {
        if (tags == null) return List.of();
        return tags.stream()
                .filter(t -> targetType == null || t.getType() == targetType)
                .map(t -> new BattleTagResponse(t.getId(), t.getName(), t.getType()))
                .toList();
    }
}
