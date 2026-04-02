package com.swyp.picke.domain.battle.service;

import com.swyp.picke.domain.battle.converter.BattleConverter;
import com.swyp.picke.domain.battle.dto.request.AdminBattleCreateRequest;
import com.swyp.picke.domain.battle.dto.request.AdminBattleUpdateRequest;
import com.swyp.picke.domain.battle.dto.response.*;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.entity.BattleOptionTag;
import com.swyp.picke.domain.battle.entity.BattleTag;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.enums.BattleType;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.battle.repository.BattleTagRepository;
import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.repository.TagRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.VoteSide;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.UserBattleService;
import com.swyp.picke.domain.vote.entity.Vote;
import com.swyp.picke.domain.vote.repository.VoteRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.infra.s3.service.S3UploadService;
import com.swyp.picke.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
    private final UserBattleService userBattleService;

    @Override
    public Battle findById(Long battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));
        if (battle.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.BATTLE_NOT_FOUND);
        }
        return battle;
    }

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

    @Override
    public BattleListResponse getBattles(int page, int size, String type) {
        int pageNumber = Math.max(0, page - 1);
        PageRequest pageRequest = PageRequest.of(pageNumber, size);
        Page<Battle> battlePage;

        if (type == null || type.equals("ALL")) {
            battlePage = battleRepository.findByDeletedAtIsNullOrderByCreatedAtDesc(pageRequest);
        } else {
            battlePage = battleRepository.findByTypeAndDeletedAtIsNullOrderByCreatedAtDesc(
                    BattleType.valueOf(type), pageRequest);
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

        List<Battle> limitedBattles = battles.stream()
                .limit(5)
                .collect(Collectors.toList());

        List<TodayBattleResponse> items = convertToTodayResponses(limitedBattles);

        return new TodayBattleListResponse(items, items.size());
    }

    @Override
    @Transactional(readOnly = true)
    public BattleUserDetailResponse getBattleDetail(Long battleId) {
        Battle battle = findById(battleId);
        List<Tag> tags = getTagsByBattle(battle);
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);
        Map<Long, List<Tag>> optionTagsMap = battleOptionTagRepository.findByBattleWithTags(battle)
                .stream()
                .collect(Collectors.groupingBy(
                        bot -> bot.getBattleOption().getId(),
                        Collectors.mapping(BattleOptionTag::getTag, Collectors.toList())
                ));
        Long currentUserId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserBattleStatusResponse statusResponse = userBattleService.getUserBattleStatus(user, battle);
        UserBattleStep currentStep = statusResponse.step();

        Optional<Vote> optionalVote = voteRepository.findByBattleIdAndUserIdWithOption(battleId, currentUserId);
        VoteSide voteStatus = optionalVote
                .map(vote -> {
                    if (vote.getPostVoteOption() != null) {
                        return vote.getPostVoteOption().getLabel() == BattleOptionLabel.A ? VoteSide.PRO : VoteSide.CON;
                    }
                    return null;
                })
                .orElse(null);

        return battleConverter.toUserDetailResponse(
                battle, tags, options, optionTagsMap,
                battle.getTotalParticipantsCount(),
                voteStatus,
                currentStep
        );
    }

    @Override
    public BattleScenarioResponse getBattleScenario(Long battleId) {
        Battle battle = findById(battleId);
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);
        return battleConverter.toScenarioResponse(battle, options);
    }

    @Override
    public UserBattleStatusResponse getUserBattleStatus(Long battleId) {
        Battle battle = findById(battleId);
        Long currentUserId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return userBattleService.getUserBattleStatus(user, battle);
    }

    @Override
    @Transactional
    public BattleVoteResponse vote(Long battleId, Long optionId) {
        Battle battle = findById(battleId);
        BattleOption option = battleOptionRepository.findById(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        Long currentUserId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        voteRepository.save(Vote.builder()
                .user(user)
                .battle(battle)
                .postVoteOption(option)
                .build());

        userBattleService.upsertStep(user, battle, UserBattleStep.PRE_VOTE);
        battle.addParticipant();
        option.increaseVoteCount();

        List<OptionStatResponse> results = calculateOptionStats(battle);
        return new BattleVoteResponse(battle.getId(), option.getId(), battle.getTotalParticipantsCount(), results);
    }

    private List<OptionStatResponse> calculateOptionStats(Battle battle) {
        return battleOptionRepository.findByBattle(battle).stream().map(option -> {
            Long voteCount = option.getVoteCount() == null ? 0L : option.getVoteCount();
            Long totalCount = battle.getTotalParticipantsCount() == null ? 0L : battle.getTotalParticipantsCount();
            Double ratio = (totalCount == 0L) ? 0.0 : Math.round((double) voteCount / totalCount * 1000) / 10.0;
            return new OptionStatResponse(option.getId(), option.getLabel(), option.getTitle(), voteCount, ratio);
        }).toList();
    }

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
        for (var optionRequest : request.options()) {
            BattleOption option = battleOptionRepository.save(BattleOption.builder()
                    .battle(battle)
                    .label(optionRequest.label())
                    .title(optionRequest.title())
                    .stance(optionRequest.stance())
                    .representative(optionRequest.representative())
                    .quote(optionRequest.quote())
                    .imageUrl(optionRequest.imageUrl())
                    .build());

            if (optionRequest.tagIds() != null) {
                saveBattleOptionTags(option, optionRequest.tagIds().stream().distinct().toList());
            }
            savedOptions.add(option);
        }

        Map<Long, List<Tag>> optionTagsMap = battleOptionTagRepository.findByBattleWithTags(battle)
                .stream()
                .collect(Collectors.groupingBy(
                        bot -> bot.getBattleOption().getId(),
                        Collectors.mapping(BattleOptionTag::getTag, Collectors.toList())
                ));

        return battleConverter.toAdminDetailResponse(battle, getTagsByBattle(battle), savedOptions, optionTagsMap);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminBattleDetailResponse updateBattle(Long battleId, AdminBattleUpdateRequest request) {
        Battle battle = findById(battleId);

        if (battle.getThumbnailUrl() != null && !battle.getThumbnailUrl().equals(request.thumbnailUrl())) {
            s3UploadService.deleteFile(battle.getThumbnailUrl());
        }

        battle.update(
                request.title(), request.titlePrefix(), request.titleSuffix(),
                request.itemA(), request.itemADesc(), request.itemB(), request.itemBDesc(),
                request.summary(), request.description(), request.thumbnailUrl(),
                request.targetDate(), request.audioDuration(), request.status()
        );

        if (request.tagIds() != null) {
            battleTagRepository.deleteByBattle(battle);
            battleTagRepository.flush();
            saveBattleTags(battle, request.tagIds().stream().distinct().toList());
        }

        if (request.options() != null) {
            List<BattleOption> existingOptions = battleOptionRepository.findByBattle(battle);
            for (var optionRequest : request.options()) {
                existingOptions.stream()
                        .filter(option -> option.getLabel() == optionRequest.label())
                        .findFirst()
                        .ifPresent(option -> {
                            if (option.getImageUrl() != null && !option.getImageUrl().equals(optionRequest.imageUrl())) {
                                s3UploadService.deleteFile(option.getImageUrl());
                            }
                            option.update(optionRequest.title(), optionRequest.stance(),
                                    optionRequest.representative(), optionRequest.quote(), optionRequest.imageUrl());
                        });
            }
        }

        List<BattleOption> updatedOptions = battleOptionRepository.findByBattle(battle);
        Map<Long, List<Tag>> optionTagsMap = battleOptionTagRepository.findByBattleWithTags(battle)
                .stream()
                .collect(Collectors.groupingBy(
                        bot -> bot.getBattleOption().getId(),
                        Collectors.mapping(BattleOptionTag::getTag, Collectors.toList())
                ));

        return battleConverter.toAdminDetailResponse(battle, getTagsByBattle(battle), updatedOptions, optionTagsMap);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminBattleDeleteResponse deleteBattle(Long battleId) {
        Battle battle = findById(battleId);
        battle.delete();
        return new AdminBattleDeleteResponse(true, LocalDateTime.now());
    }

    private List<TodayBattleResponse> convertToTodayResponses(List<Battle> battles) {
        if (battles == null || battles.isEmpty()) return Collections.emptyList();

        Map<Long, List<BattleOption>> optionsMap = battleOptionRepository.findByBattleIn(battles)
                .stream().collect(Collectors.groupingBy(o -> o.getBattle().getId()));

        Map<Long, List<Tag>> tagsMap = battleTagRepository.findByBattleIn(battles)
                .stream().collect(Collectors.groupingBy(
                        bt -> bt.getBattle().getId(),
                        Collectors.mapping(BattleTag::getTag, Collectors.toList())
                ));

        return battles.stream().map(battle -> {
            List<Tag> tags = tagsMap.getOrDefault(battle.getId(), Collections.emptyList());
            List<BattleOption> options = optionsMap.getOrDefault(battle.getId(), Collections.emptyList());
            return battleConverter.toTodayResponse(battle, tags, options);
        }).toList();
    }

    private List<Tag> getTagsByBattle(Battle battle) {
        return battleTagRepository.findByBattle(battle).stream()
                .map(BattleTag::getTag)
                .filter(tag -> tag.getDeletedAt() == null)
                .toList();
    }

    private void saveBattleTags(Battle battle, List<Long> ids) {
        tagRepository.findAllById(ids).stream()
                .filter(tag -> tag.getDeletedAt() == null)
                .forEach(tag -> battleTagRepository.save(
                        BattleTag.builder().battle(battle).tag(tag).build()));
    }

    private void saveBattleOptionTags(BattleOption option, List<Long> tagIds) {
        tagRepository.findAllById(tagIds).stream()
                .filter(tag -> tag.getDeletedAt() == null)
                .forEach(tag -> battleOptionTagRepository.save(
                        BattleOptionTag.builder().battleOption(option).tag(tag).build()));
    }

    @Override
    public BattleOption findOptionById(Long optionId) {
        return battleOptionRepository.findById(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));
    }

    @Override
    public BattleOption findOptionByBattleIdAndLabel(Long battleId, BattleOptionLabel label) {
        Battle battle = findById(battleId);
        return battleOptionRepository.findByBattleAndLabel(battle, label)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));
    }
}