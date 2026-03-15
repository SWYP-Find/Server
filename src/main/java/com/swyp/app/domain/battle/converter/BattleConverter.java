package com.swyp.app.domain.battle.converter;

import com.swyp.app.domain.battle.dto.request.AdminBattleCreateRequest;
import com.swyp.app.domain.battle.dto.response.*;
import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.enums.BattleCreatorType;
import com.swyp.app.domain.battle.enums.BattleStatus;
import com.swyp.app.domain.tag.entity.Tag;
import com.swyp.app.domain.tag.enums.TagType;
import com.swyp.app.domain.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BattleConverter {

    private static final String BASE_SHARE_URL = "https://pique.app/battles/";

    // 1. 배틀 엔티티 변환 (Admin 생성용)
    public static Battle toEntity(AdminBattleCreateRequest request, User admin) {
        return Battle.builder()
                .title(request.title())
                .summary(request.summary())
                .description(request.description())
                .thumbnailUrl(request.thumbnailUrl())
                .type(request.type())
                .targetDate(request.targetDate())
                .status(BattleStatus.DRAFT)
                .creatorType(BattleCreatorType.ADMIN)
                .creator(admin)
                .build();
    }

    // 2. 오늘의 배틀 변환
    public static TodayBattleResponse toTodayResponse(Battle b, List<Tag> tags, List<BattleOption> opts) {
        return new TodayBattleResponse(
                b.getId(),
                b.getTitle(),
                b.getSummary(),
                b.getThumbnailUrl(),
                b.getType(),
                b.getAudioDuration() == null ? 0 : b.getAudioDuration(),
                BASE_SHARE_URL + b.getId(),
                toTagResponses(tags, null),
                opts.stream().map(o -> new TodayOptionResponse(
                        o.getId(), o.getLabel(), o.getTitle(), o.getRepresentative(), o.getStance(), o.getImageUrl()
                )).toList()
        );
    }

    // 관리자용 상세 정보 변환
    public static AdminBattleDetailResponse toAdminDetailResponse(Battle b, List<Tag> tags, List<BattleOption> opts) {
        return new AdminBattleDetailResponse(
                b.getId(),
                b.getTitle(),
                b.getSummary(),
                b.getDescription(),
                b.getThumbnailUrl(),
                b.getType(),
                b.getTargetDate(),
                b.getStatus(),
                b.getCreatorType(),
                toTagResponses(tags, null),
                toOptionResponses(opts),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }

    // 3. 유저용 배틀 상세 변환 (사전/사후 투표)
    public static BattleUserDetailResponse toUserDetailResponse(Battle b, List<Tag> tags, List<BattleOption> opts, Long partCount, String voteStatus) {

        BattleSummaryResponse summary = new BattleSummaryResponse(
                b.getId(),
                b.getTitle(),
                b.getSummary(),
                b.getThumbnailUrl(),
                b.getType(),
                b.getViewCount() == null ? 0 : b.getViewCount(),
                partCount == null ? 0L : partCount,
                b.getAudioDuration() == null ? 0 : b.getAudioDuration(),
                toTagResponses(tags, null),
                toOptionResponses(opts)
        );

        return new BattleUserDetailResponse(
                summary,
                b.getDescription(),
                BASE_SHARE_URL + b.getId(),
                voteStatus,
                toTagResponses(tags, TagType.CATEGORY),
                toTagResponses(tags, TagType.PHILOSOPHER),
                toTagResponses(tags, TagType.VALUE)
        );
    }

    // 옵션 변환 (A, B, C, D 모두 대응)
    private static List<BattleOptionResponse> toOptionResponses(List<BattleOption> options) {
        return options.stream()
                .map(o -> new BattleOptionResponse(
                        o.getId(),
                        o.getLabel(),
                        o.getTitle(),
                        o.getRepresentative(),
                        o.getImageUrl(),
                        o.getStance(),
                        o.getQuote()
                )).toList();
    }

    private static List<BattleTagResponse> toTagResponses(List<Tag> tags, TagType targetType) {
        return tags.stream()
                .filter(t -> targetType == null || t.getType() == targetType)
                .map(t -> new BattleTagResponse(t.getId(), t.getName(), t.getType()))
                .toList();
    }
}