package com.swyp.app.domain.battle.service;

import com.swyp.app.domain.battle.converter.BattleConverter;
import com.swyp.app.domain.battle.dto.request.AdminBattleCreateRequest;
import com.swyp.app.domain.battle.dto.request.AdminBattleUpdateRequest;
import com.swyp.app.domain.battle.dto.response.*;
import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.entity.BattleTag;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import com.swyp.app.domain.battle.enums.BattleStatus;
import com.swyp.app.domain.battle.enums.BattleType;
import com.swyp.app.domain.battle.repository.BattleOptionRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleServiceImpl implements BattleService {

    private final BattleRepository battleRepository;
    private final BattleOptionRepository battleOptionRepository;
    private final BattleTagRepository battleTagRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;

    @Override
    public Battle findById(UUID battleId) {
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
    public List<TodayBattleResponse> getNewBattles(List<UUID> excludeIds) {
        List<UUID> finalExcludeIds = (excludeIds == null || excludeIds.isEmpty())
                ? List.of(UUID.randomUUID()) : excludeIds;
        List<Battle> battles = battleRepository.findNewBattlesExcluding(finalExcludeIds, PageRequest.of(0, 10));
        return convertToTodayResponses(battles);
    }

    // [사용자용 - 기본 API]

    @Override
    public TodayBattleListResponse getTodayBattles() {
        List<Battle> battles = battleRepository.findByTargetDateAndStatusAndDeletedAtIsNull(
                LocalDate.now(), BattleStatus.PUBLISHED);
        List<TodayBattleResponse> items = convertToTodayResponses(battles);
        return new TodayBattleListResponse(items, items.size());
    }

    @Override
    public BattleUserDetailResponse getBattleDetail(UUID battleId) {
        Battle battle = findById(battleId);
        battle.increaseViewCount();

        List<Tag> allTags = getTagsByBattle(battle);
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);

        // 🔥 수정됨: findByBattleIdAndUserId -> findByBattleAndUserId 로 변경하여 에러 해결
        String voteStatus = voteRepository.findByBattleAndUserId(battle, 1L)
                .map(v -> {
                    if (v.getPostVoteOption() != null) {
                        return v.getPostVoteOption().getLabel().name();
                    }
                    return "NONE"; // 사후 투표를 아직 안 했을 경우를 대비한 안전 처리
                })
                .orElse("NONE");

        return BattleConverter.toUserDetailResponse(battle, allTags, options, battle.getTotalParticipantsCount(), voteStatus);
    }

    @Override
    @Transactional
    public BattleVoteResponse vote(UUID battleId, UUID optionId) {
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
    public AdminBattleDetailResponse createBattle(AdminBattleCreateRequest request, Long adminUserId) {
        // 1. 유저 확인
        User admin = userRepository.findById(adminUserId == null ? 1L : adminUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 선택지 개수 검증 (VOTE는 4개, QUIZ, BATTLE은 2개)
        int requiredOptionCount = (request.type() == BattleType.VOTE) ? 4 : 2;
        if (request.options().size() != requiredOptionCount) {
            throw new CustomException(ErrorCode.BATTLE_INVALID_OPTION_COUNT);
        }

        // 3. 배틀 저장
        Battle battle = battleRepository.save(BattleConverter.toEntity(request, admin));

        // 4. 태그 저장
        if (request.tagIds() != null) {
            saveBattleTags(battle, request.tagIds());
        }

        // 5. 옵션 저장
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
            savedOptions.add(option);
        }

        // 6. 관리자용 상세 응답 반환
        return BattleConverter.toAdminDetailResponse(battle, getTagsByBattle(battle), savedOptions);
    }

    @Override
    @Transactional
    public AdminBattleDetailResponse updateBattle(UUID battleId, AdminBattleUpdateRequest request) {
        Battle battle = findById(battleId);
        battle.update(request.title(), request.summary(), request.description(),
                request.thumbnailUrl(), request.targetDate(), request.audioDuration(), request.status());

        if (request.tagIds() != null) {
            battleTagRepository.deleteByBattle(battle);
            saveBattleTags(battle, request.tagIds());
        }

        return BattleConverter.toAdminDetailResponse(battle, getTagsByBattle(battle), battleOptionRepository.findByBattle(battle));
    }

    @Override
    @Transactional
    public AdminBattleDeleteResponse deleteBattle(UUID battleId) {
        Battle battle = findById(battleId);
        battle.delete();
        return new AdminBattleDeleteResponse(true, LocalDateTime.now());
    }

    // [공통 헬퍼 메서드]

    private List<TodayBattleResponse> convertToTodayResponses(List<Battle> battles) {
        return battles.stream().map(battle -> {
            List<Tag> tags = getTagsByBattle(battle);
            List<BattleOption> options = battleOptionRepository.findByBattle(battle);
            return BattleConverter.toTodayResponse(battle, tags, options);
        }).toList();
    }

    private List<Tag> getTagsByBattle(Battle b) {
        return battleTagRepository.findByBattle(b).stream()
                .map(BattleTag::getTag)
                .filter(t -> t.getDeletedAt() == null)
                .toList();
    }

    private void saveBattleTags(Battle b, List<UUID> ids) {
        tagRepository.findAllById(ids).stream()
                .filter(t -> t.getDeletedAt() == null)
                .forEach(t -> battleTagRepository.save(BattleTag.builder().battle(b).tag(t).build()));
    }

    @Override
    public BattleOption findOptionById(UUID optionId) {
        return battleOptionRepository.findById(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));
    }

    @Override
    public BattleOption findOptionByBattleIdAndLabel(UUID battleId, BattleOptionLabel label) {
        Battle b = findById(battleId);
        return battleOptionRepository.findByBattleAndLabel(b, label)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));
    }
}