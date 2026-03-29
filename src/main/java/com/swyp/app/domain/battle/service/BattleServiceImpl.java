package com.swyp.app.domain.battle.service;

import com.swyp.app.domain.battle.converter.BattleConverter;
import com.swyp.app.domain.battle.dto.request.AdminBattleCreateRequest;
import com.swyp.app.domain.battle.dto.request.AdminBattleUpdateRequest;
import com.swyp.app.domain.battle.dto.response.*;
import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.entity.BattleOptionTag;
import com.swyp.app.domain.battle.entity.BattleTag;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import com.swyp.app.domain.battle.enums.BattleStatus;
import com.swyp.app.domain.battle.enums.BattleType;
import com.swyp.app.domain.battle.repository.BattleOptionRepository;
import com.swyp.app.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.app.domain.battle.repository.BattleRepository;
import com.swyp.app.domain.battle.repository.BattleTagRepository;
import com.swyp.app.domain.tag.entity.Tag;
import com.swyp.app.domain.tag.repository.TagRepository;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.domain.vote.repository.VoteRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import com.swyp.app.global.infra.s3.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleServiceImpl implements BattleService {

    private final BattleRepository battleRepository;
    private final BattleOptionRepository battleOptionRepository;
    private final BattleTagRepository battleTagRepository;
    private final BattleOptionTagRepository battleOptionTagRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final BattleConverter battleConverter;
    private final S3UploadService s3UploadService;

    @Override
    public Battle findById(Long battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        if (battle.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.BATTLE_NOT_FOUND);
        }
        return battle;
    }

    // [사용자용 - 홈 화면 5단 로직]

    @Override
    public List<TodayBattleResponse> getEditorPicks(int limit) {
        List<Battle> battles = battleRepository.findEditorPicks(BattleStatus.PUBLISHED, PageRequest.of(0, limit));
        return convertToTodayResponses(battles);
    }

    @Override
    public List<TodayBattleResponse> getTrendingBattles(int limit) {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<Battle> battles = battleRepository.findTrendingBattles(yesterday, PageRequest.of(0, limit));
        return convertToTodayResponses(battles);
    }

    @Override
    public List<TodayBattleResponse> getBestBattles(int limit) {
        List<Battle> battles = battleRepository.findBestBattles(PageRequest.of(0, limit));
        return convertToTodayResponses(battles);
    }

    @Override
    public List<TodayBattleResponse> getTodayPicks(BattleType type, int limit) {
        // findTodayPicks 레포지토리 메서드에 Pageable을 이미 추가하셨다면 문제없이 동작합니다!
        List<Battle> battles = battleRepository.findTodayPicks(type, LocalDate.now(), PageRequest.of(0, limit));
        return convertToTodayResponses(battles);
    }

    @Override
    public List<TodayBattleResponse> getNewBattles(List<Long> excludeIds, int limit) {
        List<Long> finalExcludeIds = (excludeIds == null || excludeIds.isEmpty())
                ? List.of(-1L) : excludeIds;
        List<Battle> battles = battleRepository.findNewBattlesExcluding(finalExcludeIds, PageRequest.of(0, limit));
        return convertToTodayResponses(battles);
    }

    // [사용자용 - 기본 API]

    @Override
    public BattleListResponse getBattles(int page, int size, String type) {
        int pageNumber = Math.max(0, page - 1);
        PageRequest pageRequest = PageRequest.of(pageNumber, size);
        Page<Battle> battlePage;

        // type이 ALL이거나 없으면 전체 조회, 아니면 타입별 조회
        if (type == null || type.equals("ALL")) {
            battlePage = battleRepository.findByDeletedAtIsNullOrderByCreatedAtDesc(pageRequest);
        } else {
            battlePage = battleRepository.findByTypeAndDeletedAtIsNullOrderByCreatedAtDesc(BattleType.valueOf(type), pageRequest);
        }

        List<BattleSimpleResponse> items = battlePage.getContent().stream()
                .map(battleConverter::toSimpleResponse)
                .toList();

        return new BattleListResponse(
                items,
                battlePage.getNumber() + 1,
                battlePage.getTotalPages(),
                battlePage.getTotalElements()
        );
    }

    @Override
    public TodayBattleListResponse getTodayBattles() {
        List<Battle> battles = battleRepository.findByTargetDateAndStatusAndDeletedAtIsNull(
                LocalDate.now(), BattleStatus.PUBLISHED);
        List<TodayBattleResponse> items = convertToTodayResponses(battles);
        return new TodayBattleListResponse(items, items.size());
    }

    // [사용자용 상세 조회] - 썸네일 + 철학자 이미지 보안 처리
    @Override
    @Transactional(readOnly = true)
    public BattleUserDetailResponse getBattleDetail(Long battleId) {
        Battle battle = findById(battleId);
        List<Tag> tags = getTagsByBattle(battle);
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);

        // 1. 썸네일 보안 URL 생성
        String secureThumbnail = s3UploadService.getPresignedUrl(battle.getThumbnailUrl(), Duration.ofMinutes(10));
        String voteStatus = voteRepository.findByBattleIdAndUserId(battleId, 1L)
                .map(v -> v.getPostVoteOption() != null ? v.getPostVoteOption().getLabel().name() : "NONE")
                .orElse("NONE");

        // 2. 컨버터를 통해 전체 조립 (철학자 이미지는 컨버터 내부에서 s3UploadService로 처리)
        return battleConverter.toUserDetailResponse(
                battle,
                tags,
                options,
                battle.getTotalParticipantsCount(),
                "NONE",
                secureThumbnail,
                s3UploadService // 철학자 이미지 변환을 위해 전달
        );
    }

    @Override
    @Transactional
    public BattleVoteResponse vote(Long battleId, Long optionId) {
        Battle battle = findById(battleId);
        BattleOption option = battleOptionRepository.findById(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        battle.addParticipant();
        option.increaseVoteCount();

        List<OptionStatResponse> results = battleOptionRepository.findByBattle(battle).stream().map(opt -> {
            Long v = opt.getVoteCount() == null ? 0L : opt.getVoteCount();
            Long t = battle.getTotalParticipantsCount() == null ? 0L : battle.getTotalParticipantsCount();
            Double r = (t == 0L) ? 0.0 : Math.round((double) v / t * 1000) / 10.0;
            return new OptionStatResponse(opt.getId(), opt.getLabel(), opt.getTitle(), v, r);
        }).toList();

        return new BattleVoteResponse(battle.getId(), option.getId(), battle.getTotalParticipantsCount(), results);
    }

    // [관리자용 API]

    // [관리자용 생성] - 생성 직후 결과 화면에서도 이미지가 보이게 처리
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminBattleDetailResponse createBattle(AdminBattleCreateRequest request, Long adminUserId) {
        User admin = userRepository.findById(adminUserId == null ? 1L : adminUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Battle battle = battleRepository.save(battleConverter.toEntity(request, admin));

        if (request.tagIds() != null) {
            saveBattleTags(battle, request.tagIds().stream().distinct().toList());
        }

        List<BattleOption> savedOptions = new ArrayList<>();
        for (var optReq : request.options()) {
            BattleOption option = battleOptionRepository.save(BattleOption.builder()
                    .battle(battle)
                    .label(optReq.label())
                    .title(optReq.title())
                    .stance(optReq.stance())
                    .representative(optReq.representative())
                    .quote(optReq.quote())
                    .imageUrl(optReq.imageUrl())
                    .build());

            if (optReq.tagIds() != null) {
                saveBattleOptionTags(option, optReq.tagIds().stream().distinct().toList());
            }
            savedOptions.add(option);
        }

        // 생성 후 응답 시 s3UploadService 전달
        return battleConverter.toAdminDetailResponse(battle, getTagsByBattle(battle), savedOptions, s3UploadService);
    }

    private void saveBattleOptionTags(BattleOption option, List<Long> tagIds) {
        tagRepository.findAllById(tagIds).stream()
                .filter(t -> t.getDeletedAt() == null)
                .forEach(t -> battleOptionTagRepository.save(
                        BattleOptionTag.builder().battleOption(option).tag(t).build()
                ));
    }

    // [관리자용 수정] - 수정 완료 후 결과 화면에서도 이미지가 보이게 처리
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminBattleDetailResponse updateBattle(Long battleId, AdminBattleUpdateRequest request) {
        Battle battle = findById(battleId);

        // 썸네일 이미지가 변경되었다면 기존 S3 파일 삭제 (스토리지 낭비 방지)
        if (battle.getThumbnailUrl() != null && !battle.getThumbnailUrl().equals(request.thumbnailUrl())) {
            s3UploadService.deleteFile(battle.getThumbnailUrl());
        }

        // 배틀 필드 업데이트
        battle.update(
                request.title(), request.titlePrefix(), request.titleSuffix(),
                request.itemA(), request.itemADesc(), request.itemB(), request.itemBDesc(),
                request.summary(), request.description(), request.thumbnailUrl(),
                request.targetDate(), request.audioDuration(), request.status()
        );

        // 태그 업데이트
        if (request.tagIds() != null) {
            battleTagRepository.deleteByBattle(battle);
            battleTagRepository.flush(); // DB에 DELETE 쿼리를 즉시 전송해서 완전히 비워버림

            // request.tagIds()에 혹시 모를 중복값이 있으면 distinct()로 제거하고 저장
            saveBattleTags(battle, request.tagIds().stream().distinct().toList());
        }

        // 3. 선택지 업데이트
        if (request.options() != null) {
            List<BattleOption> existingOptions = battleOptionRepository.findByBattle(battle);
            for (var optReq : request.options()) {
                existingOptions.stream()
                        .filter(o -> o.getLabel() == optReq.label())
                        .findFirst()
                        .ifPresent(o -> {
                            // 철학자/선택지 이미지가 변경되었다면 기존 S3 파일 삭제
                            if (o.getImageUrl() != null && !o.getImageUrl().equals(optReq.imageUrl())) {
                                s3UploadService.deleteFile(o.getImageUrl());
                            }

                            o.update(optReq.title(), optReq.stance(), optReq.representative(), optReq.quote(), optReq.imageUrl());
                        });
            }
        }

        List<BattleOption> updatedOptions = battleOptionRepository.findByBattle(battle);
        // 업데이트 후 응답 시 s3UploadService 전달
        return battleConverter.toAdminDetailResponse(battle, getTagsByBattle(battle), updatedOptions, s3UploadService);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminBattleDeleteResponse deleteBattle(Long battleId) {
        Battle battle = findById(battleId);
        battle.delete();
        return new AdminBattleDeleteResponse(true, LocalDateTime.now());
    }

    // [공통 헬퍼 메서드]

    // N+1 개선 버전
    private List<TodayBattleResponse> convertToTodayResponses(List<Battle> battles) {
        if (battles == null || battles.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. IN 쿼리로 모든 옵션과 태그를 한 번에 가져와서 배틀 ID별로 그룹핑
        Map<Long, List<BattleOption>> optionsMap = battleOptionRepository.findByBattleIn(battles)
                .stream().collect(Collectors.groupingBy(battleOption -> battleOption.getBattle().getId()));

        Map<Long, List<Tag>> tagsMap = battleTagRepository.findByBattleIn(battles)
                .stream().collect(Collectors.groupingBy(
                        battleTag -> battleTag.getBattle().getId(),
                        Collectors.mapping(BattleTag::getTag, Collectors.toList())
                ));

        // 2. DB 쿼리 없이 메모리(Map)에서 꺼내서 조립만 수행
        return battles.stream().map(battle -> {
            List<Tag> tags = tagsMap.getOrDefault(battle.getId(), Collections.emptyList());
            List<BattleOption> options = optionsMap.getOrDefault(battle.getId(), Collections.emptyList());

            return battleConverter.toTodayResponse(battle, tags, options);
        }).toList();
    }

    private List<Tag> getTagsByBattle(Battle b) {
        return battleTagRepository.findByBattle(b).stream()
                .map(BattleTag::getTag)
                .filter(t -> t.getDeletedAt() == null)
                .toList();
    }

    private void saveBattleTags(Battle b, List<Long> ids) {
        tagRepository.findAllById(ids).stream()
                .filter(t -> t.getDeletedAt() == null)
                .forEach(t -> battleTagRepository.save(BattleTag.builder().battle(b).tag(t).build()));
    }

    @Override
    public BattleOption findOptionById(Long optionId) {
        return battleOptionRepository.findById(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));
    }

    @Override
    public BattleOption findOptionByBattleIdAndLabel(Long battleId, BattleOptionLabel label) {
        Battle b = findById(battleId);
        return battleOptionRepository.findByBattleAndLabel(b, label)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));
    }
}