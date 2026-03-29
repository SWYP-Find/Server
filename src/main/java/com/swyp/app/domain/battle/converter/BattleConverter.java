package com.swyp.app.domain.battle.converter;

import com.swyp.app.domain.battle.dto.request.AdminBattleCreateRequest;
import com.swyp.app.domain.battle.dto.response.*;
import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.entity.BattleOptionTag;
import com.swyp.app.domain.battle.enums.BattleCreatorType;
import com.swyp.app.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.app.domain.tag.entity.Tag;
import com.swyp.app.domain.tag.enums.TagType;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.global.infra.s3.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
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
                .status(request.status())
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

    // 관리자용 상세 응답 변환 (보안 URL 적용)
    public AdminBattleDetailResponse toAdminDetailResponse(
            Battle b, List<Tag> tags, List<BattleOption> opts, S3UploadService s3Service) {

        // 썸네일 보안 URL
        String secureThumbnail = (b.getThumbnailUrl() != null && !b.getThumbnailUrl().isBlank())
                ? s3Service.getPresignedUrl(b.getThumbnailUrl(), Duration.ofMinutes(10))
                : null;

        return new AdminBattleDetailResponse(
                b.getId(),
                b.getTitle(),
                b.getTitlePrefix(),
                b.getTitleSuffix(),
                b.getSummary(),
                b.getDescription(),
                secureThumbnail,
                b.getType(),
                b.getItemA(),
                b.getItemADesc(),
                b.getItemB(),
                b.getItemBDesc(),
                b.getTargetDate(),
                b.getStatus(),
                b.getCreatorType(),
                toTagResponses(tags, null),
                toOptionResponses(opts, s3Service),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }

    // 유저 상세 응답 변환
    public BattleUserDetailResponse toUserDetailResponse(
            Battle b, List<Tag> tags, List<BattleOption> opts,
            Long partCount, String voteStatus, String secureThumbnail,
            S3UploadService s3Service) {

        BattleSummaryResponse summary = new BattleSummaryResponse(
                b.getId(),
                b.getTitle(),
                b.getSummary(),
                secureThumbnail,
                b.getType(),
                b.getViewCount() == null ? 0 : b.getViewCount(),
                partCount == null ? 0L : partCount,
                b.getAudioDuration() == null ? 0 : b.getAudioDuration(),
                toTagResponses(tags, null),
                toOptionResponses(opts, s3Service)
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

    // 철학자 이미지 보안 처리를 포함한 옵션 응답 변환
    private List<BattleOptionResponse> toOptionResponses(List<BattleOption> options, S3UploadService s3Service) {
        if (options == null) return List.of();

        return options.stream()
                .map(o -> {
                    List<Tag> optionTags = optionTagRepository.findByBattleOption(o).stream()
                            .map(BattleOptionTag::getTag)
                            .toList();

                    // 철학자 이미지 방어 로직 (null/공백일 경우 s3Service 호출 안 함)
                    String securePhilosopherImg = (o.getImageUrl() != null && !o.getImageUrl().isBlank())
                            ? s3Service.getPresignedUrl(o.getImageUrl(), Duration.ofMinutes(10))
                            : null;

                    return new BattleOptionResponse(
                            o.getId(),
                            o.getLabel(),
                            o.getTitle(),
                            o.getStance(),
                            o.getRepresentative(),
                            o.getQuote(),
                            securePhilosopherImg,
                            toTagResponses(optionTags, null)
                    );
                }).toList();
    }

    // 투데이 옵션 응답 변환
    private List<TodayOptionResponse> toTodayOptionResponses(List<BattleOption> options) {
        if (options == null) return List.of();
        return options.stream().map(o -> new TodayOptionResponse(
                o.getId(), o.getLabel(), o.getTitle(), o.getRepresentative(), o.getStance(), o.getImageUrl()
        )).toList();
    }

    // 태그 응답 변환
    private List<BattleTagResponse> toTagResponses(List<Tag> tags, TagType targetType) {
        if (tags == null) return List.of();
        return tags.stream()
                .filter(t -> targetType == null || t.getType() == targetType)
                .map(t -> new BattleTagResponse(t.getId(), t.getName(), t.getType()))
                .toList();
    }
}