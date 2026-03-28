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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    public List<TodayBattleResponse> getEditorPicks() {
        List<Battle> battles = battleRepository.findEditorPicks(BattleStatus.PUBLISHED, PageRequest.of(0, 10));
        return convertToTodayResponses(battles);
    }

    @Override
    public List<TodayBattleResponse> getTrendingBattles() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<Battle> battles = battleRepository.findTrendingBattles(yesterday, PageRequest.of(0, 5));
        return convertToTodayResponses(battles);
    }

    @Override
    public List<TodayBattleResponse> getBestBattles() {
        List<Battle> battles = battleRepository.findBestBattles(PageRequest.of(0, 5));
        return convertToTodayResponses(battles);
    }

    @Override
    public List<TodayBattleResponse> getTodayPicks(BattleType type) {
        List<Battle> battles = battleRepository.findTodayPicks(type, LocalDate.now());
        return convertToTodayResponses(battles);
    }

    @Override
    public List<TodayBattleResponse> getNewBattles(List<Long> excludeIds) {
        List<Long> finalExcludeIds = (excludeIds == null || excludeIds.isEmpty())
                ? List.of(-1L) : excludeIds;
        List<Battle> battles = battleRepository.findNewBattlesExcluding(finalExcludeIds, PageRequest.of(0, 10));
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

    @Override
    public BattleUserDetailResponse getBattleDetail(Long battleId) {
        Battle battle = findById(battleId);
        battle.increaseViewCount();

        List<Tag> allTags = getTagsByBattle(battle);
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);

        String voteStatus = voteRepository.findByBattleAndUserId(battle, 1L)
                .map(v -> v.getPostVoteOption() != null ? v.getPostVoteOption().getLabel().name() : "NONE")
                .orElse("NONE");

        return battleConverter.toUserDetailResponse(battle, allTags, options, battle.getTotalParticipantsCount(), voteStatus);
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
        return battleConverter.toAdminDetailResponse(battle, getTagsByBattle(battle), savedOptions);
    }

    private void saveBattleOptionTags(BattleOption option, List<Long> tagIds) {
        tagRepository.findAllById(tagIds).stream()
                .filter(t -> t.getDeletedAt() == null)
                .forEach(t -> battleOptionTagRepository.save(
                        BattleOptionTag.builder().battleOption(option).tag(t).build()
                ));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminBattleDetailResponse updateBattle(Long battleId, AdminBattleUpdateRequest request) {
        // [STEP 2] 서버 터미널에 출력
        System.out.println("====== [백엔드 수신 로그] ======");
        System.out.println("ID: " + battleId);
        System.out.println("제목: " + request.title());
        System.out.println("공연A: " + request.itemA());
        System.out.println("A설명: " + request.itemADesc());
        System.out.println("선택지A: " + (request.options() != null ? request.options().get(0).title() : "null"));
        System.out.println("==============================");

        Battle battle = findById(battleId);

        // 1. 배틀 필드 업데이트
        battle.update(
                request.title(),
                request.titlePrefix(),
                request.titleSuffix(),
                request.itemA(),
                request.itemADesc(),
                request.itemB(),
                request.itemBDesc(),
                request.summary(),
                request.description(),
                request.thumbnailUrl(),
                request.targetDate(),
                request.audioDuration(),
                request.status()
        );

        // 2. 태그 업데이트
        if (request.tagIds() != null) {
            battleTagRepository.deleteByBattle(battle);
            saveBattleTags(battle, request.tagIds());
        }

        // 3. 선택지 업데이트
        if (request.options() != null) {
            List<BattleOption> existingOptions = battleOptionRepository.findByBattle(battle);
            for (var optReq : request.options()) {
                existingOptions.stream()
                        .filter(o -> o.getLabel() == optReq.label())
                        .findFirst()
                        .ifPresent(o -> {
                            o.update(optReq.title(), optReq.stance(), optReq.representative(), optReq.quote(), optReq.imageUrl());
                        });
            }
        }

        // 변경된 옵션 다시 조회해서 응답 포함
        List<BattleOption> updatedOptions = battleOptionRepository.findByBattle(battle);
        return battleConverter.toAdminDetailResponse(battle, getTagsByBattle(battle), updatedOptions);
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

    private List<TodayBattleResponse> convertToTodayResponses(List<Battle> battles) {
        return battles.stream().map(battle -> {
            List<Tag> tags = getTagsByBattle(battle);
            List<BattleOption> options = battleOptionRepository.findByBattle(battle);
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
