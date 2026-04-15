package com.swyp.picke.domain.battle.service;

import com.swyp.picke.domain.battle.converter.BattleConverter;
import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleCreateRequest;
import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleOptionRequest;
import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleUpdateRequest;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleDeleteResponse;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleDetailResponse;
import com.swyp.picke.domain.battle.dto.response.*;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.entity.BattleOptionTag;
import com.swyp.picke.domain.battle.entity.BattleTag;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.battle.repository.BattleTagRepository;
import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.tag.repository.TagRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.VoteSide;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.UserBattleService;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.infra.local.service.LocalDraftFileStorageService;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
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
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleServiceImpl implements BattleService {

    private static final int HOME_EDITOR_PICK_LIMIT = 10;
    private static final int HOME_TRENDING_LIMIT = 4;
    private static final int HOME_BEST_LIMIT = 3;
    private static final int HOME_TODAY_PICK_LIMIT = 1;
    private static final int HOME_NEW_LIMIT = 3;
    private static final Pattern RESOURCE_IMAGE_PATH_PATTERN = Pattern.compile("/api/v1/resources/images/([A-Z_]+)/(.+)");

    private final BattleRepository battleRepository;
    private final BattleOptionRepository battleOptionRepository;
    private final BattleTagRepository battleTagRepository;
    private final BattleOptionTagRepository battleOptionTagRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final BattleVoteRepository battleVoteRepository;
    private final BattleConverter battleConverter;
    private final S3UploadService s3UploadService;
    private final LocalDraftFileStorageService localDraftFileStorageService;
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
    public List<TodayBattleResponse> getEditorPicks() {
        return loadEditorPicks(HOME_EDITOR_PICK_LIMIT);
    }

    @Override
    public List<TodayBattleResponse> getTrendingBattles() {
        return loadTrendingBattles(HOME_TRENDING_LIMIT);
    }

    @Override
    public List<TodayBattleResponse> getBestBattles() {
        return loadBestBattles(HOME_BEST_LIMIT);
    }

    @Override
    @Transactional
    public List<TodayBattleResponse> getTodayPicks() {
        return loadTodayPicks(HOME_TODAY_PICK_LIMIT);
    }

    @Override
    public List<TodayBattleResponse> getNewBattles(List<Long> excludeIds) {
        return loadNewBattles(excludeIds, HOME_NEW_LIMIT);
    }

    private List<TodayBattleResponse> loadEditorPicks(int limit) {
        int safeLimit = Math.max(1, limit);
        List<Battle> battles = battleRepository.findEditorPicks(BattleStatus.PUBLISHED, PageRequest.of(0, safeLimit));
        return convertToTodayResponses(battles);
    }

    private List<TodayBattleResponse> loadTrendingBattles(int limit) {
        int safeLimit = Math.max(1, limit);
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<Battle> battles = battleRepository.findTrendingBattles(yesterday, PageRequest.of(0, safeLimit));
        return convertToTodayResponses(battles);
    }

    private List<TodayBattleResponse> loadBestBattles(int limit) {
        int safeLimit = Math.max(1, limit);
        List<Battle> battles = battleRepository.findBestBattles(PageRequest.of(0, safeLimit));
        return convertToTodayResponses(battles);
    }

    private List<TodayBattleResponse> loadTodayPicks(int limit) {
        int safeLimit = Math.max(1, limit);
        LocalDate today = LocalDate.now();
        ensureTodayPicks(today, safeLimit);

        List<Battle> battles = battleRepository.findTodayPicks(today, PageRequest.of(0, safeLimit));
        return convertToTodayResponses(battles);
    }

    private List<TodayBattleResponse> loadNewBattles(List<Long> excludeIds, int limit) {
        int safeLimit = Math.max(1, limit);
        List<Long> finalExcludeIds = (excludeIds == null || excludeIds.isEmpty())
                ? List.of(-1L) : excludeIds;
        List<Battle> battles = battleRepository.findNewBattlesExcluding(finalExcludeIds, PageRequest.of(0, safeLimit));
        return convertToTodayResponses(battles);
    }

    @Override
    public BattleListResponse getBattles(int page, int size, String status) {
        int pageNumber = Math.max(0, page - 1);
        PageRequest pageRequest = PageRequest.of(pageNumber, size);
        BattleStatus battleStatusFilter = parseBattleStatus(status);

        Page<Battle> battlePage;
        if (battleStatusFilter == null) {
            battlePage = battleRepository.findByDeletedAtIsNullOrderByCreatedAtDesc(pageRequest);
        } else {
            battlePage = battleRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                    battleStatusFilter,
                    pageRequest
            );
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
    @Transactional
    public TodayBattleListResponse getTodayBattles() {
        LocalDate today = LocalDate.now();
        ensureTodayPicks(today, 5);
        List<Battle> battles = battleRepository.findByTargetDateAndStatusAndDeletedAtIsNull(today, BattleStatus.PUBLISHED);

        List<Battle> limitedBattles = battles.stream()
                .limit(5)
                .collect(Collectors.toList());

        List<TodayBattleResponse> items = convertToTodayResponses(limitedBattles);

        return new TodayBattleListResponse(items, items.size());
    }

    private void ensureTodayPicks(LocalDate today, int requiredCount) {
        List<Battle> todays = battleRepository.findTodayPicks(today, PageRequest.of(0, requiredCount));
        int missingCount = requiredCount - todays.size();
        if (missingCount <= 0) return;

        List<Battle> candidates = battleRepository.findAutoAssignableTodayPicks(today, PageRequest.of(0, missingCount));
        for (Battle candidate : candidates) {
            candidate.updateTargetDate(today);
        }
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

        Optional<BattleVote> optionalVote = battleVoteRepository.findByBattleIdAndUserIdWithOption(battleId, currentUserId);
        VoteSide voteStatus = optionalVote
                .map(BattleVote -> {
                    if (BattleVote.getPostVoteOption() != null) {
                        return BattleVote.getPostVoteOption().getLabel() == BattleOptionLabel.A ? VoteSide.PRO : VoteSide.CON;
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
    public BattleVoteResponse BattleVote(Long battleId, Long optionId) {
        Battle battle = findById(battleId);
        BattleOption newOption = battleOptionRepository.findById(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        Long currentUserId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        battleVoteRepository.save(BattleVote.builder()
                .user(user)
                .battle(battle)
                .preVoteOption(newOption)
                .isTtsListened(false)
                .build());

        userBattleService.upsertStep(user, battle, UserBattleStep.PRE_VOTE);
        List<OptionStatResponse> results = calculateOptionStats(battle);
        return new BattleVoteResponse(battle.getId(), newOption.getId(), battle.getTotalParticipantsCount(), results);
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

        validateBattleOptionCount(request.options());

        String resolvedThumbnailKey = resolveStoredImageKey(request.thumbnailUrl(), request.status(), FileCategory.BATTLE);
        Battle battle = battleConverter.toEntity(request, admin);
        battle.update(
                request.title(),
                request.summary(),
                request.description(),
                resolvedThumbnailKey,
                request.targetDate(),
                request.audioDuration(),
                request.status()
        );
        battle = battleRepository.save(battle);

        if (request.tagIds() != null) {
            saveBattleTags(battle, request.tagIds().stream().distinct().toList());
        }

        List<BattleOption> savedOptions = new ArrayList<>();
        if (request.options() != null) {
            for (AdminBattleOptionRequest optionRequest : request.options()) {
                String resolvedImageKey = resolveStoredImageKey(
                        optionRequest.imageUrl(),
                        request.status(),
                        FileCategory.PHILOSOPHER
                );
                BattleOption option = BattleOption.builder()
                        .battle(battle)
                        .label(optionRequest.label())
                        .title(optionRequest.title())
                        .stance(optionRequest.stance())
                        .representative(optionRequest.representative())
                        .imageUrl(resolvedImageKey)
                        .build();
                option = battleOptionRepository.save(option);

                if (optionRequest.tagIds() != null) {
                    saveBattleOptionTags(option, optionRequest.tagIds().stream().distinct().toList());
                }
                savedOptions.add(option);
            }
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
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public AdminBattleDetailResponse getAdminBattleDetail(Long battleId) {
        Battle battle = findById(battleId);
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);
        Map<Long, List<Tag>> optionTagsMap = battleOptionTagRepository.findByBattleWithTags(battle)
                .stream()
                .collect(Collectors.groupingBy(
                        bot -> bot.getBattleOption().getId(),
                        Collectors.mapping(BattleOptionTag::getTag, Collectors.toList())
                ));

        return battleConverter.toAdminDetailResponse(battle, getTagsByBattle(battle), options, optionTagsMap);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminBattleDetailResponse updateBattle(Long battleId, AdminBattleUpdateRequest request) {
        Battle battle = findById(battleId);
        validateBattleOptionCount(request.options());

        String existingThumbnailKey = normalizeStoredImageReference(battle.getThumbnailUrl(), FileCategory.BATTLE);
        String resolvedThumbnailKey = resolveStoredImageKey(request.thumbnailUrl(), request.status(), FileCategory.BATTLE);
        if (existingThumbnailKey != null && !existingThumbnailKey.equals(resolvedThumbnailKey)) {
            deleteStoredAsset(existingThumbnailKey);
        }

        battle.update(
                request.title(),
                request.summary(),
                request.description(),
                resolvedThumbnailKey,
                request.targetDate(),
                request.audioDuration(),
                request.status()
        );

        if (request.tagIds() != null) {
            battleTagRepository.deleteByBattle(battle);
            battleTagRepository.flush();
            saveBattleTags(battle, request.tagIds().stream().distinct().toList());
        }

        if (request.options() != null) {
            List<BattleOption> existingOptions = battleOptionRepository.findByBattle(battle);
            Map<BattleOptionLabel, BattleOption> existingOptionMap = existingOptions.stream()
                    .collect(Collectors.toMap(BattleOption::getLabel, option -> option));

            Set<BattleOptionLabel> requestedLabels = new HashSet<>();

            for (AdminBattleOptionRequest optionRequest : request.options()) {
                requestedLabels.add(optionRequest.label());

                BattleOption option = existingOptionMap.get(optionRequest.label());
                String resolvedOptionImageKey = resolveStoredImageKey(
                        optionRequest.imageUrl(),
                        request.status(),
                        FileCategory.PHILOSOPHER
                );
                if (option == null) {
                    option = BattleOption.builder()
                            .battle(battle)
                            .label(optionRequest.label())
                            .title(optionRequest.title())
                            .stance(optionRequest.stance())
                            .representative(optionRequest.representative())
                            .imageUrl(resolvedOptionImageKey)
                            .build();
                    option = battleOptionRepository.save(option);
                } else {
                    String existingOptionImageKey = normalizeStoredImageReference(option.getImageUrl(), FileCategory.PHILOSOPHER);
                    if (existingOptionImageKey != null && !existingOptionImageKey.equals(resolvedOptionImageKey)) {
                        deleteStoredAsset(existingOptionImageKey);
                    }
                    option.update(optionRequest.title(), optionRequest.stance(),
                            optionRequest.representative(), resolvedOptionImageKey);
                }

                replaceBattleOptionTags(option, optionRequest.tagIds());
            }

            List<BattleOption> removedOptions = existingOptions.stream()
                    .filter(existing -> !requestedLabels.contains(existing.getLabel()))
                    .toList();

            for (BattleOption removedOption : removedOptions) {
                deleteStoredAsset(removedOption.getImageUrl());
                List<BattleOptionTag> optionTags = battleOptionTagRepository.findByBattleOption(removedOption);
                if (!optionTags.isEmpty()) {
                    battleOptionTagRepository.deleteAll(optionTags);
                }
            }

            if (!removedOptions.isEmpty()) {
                battleOptionRepository.deleteAll(removedOptions);
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
                .filter(tag -> tag.getType() == TagType.CATEGORY)
                .forEach(tag -> battleTagRepository.save(
                        BattleTag.builder().battle(battle).tag(tag).build()));
    }

    private void saveBattleOptionTags(BattleOption option, List<Long> tagIds) {
        tagRepository.findAllById(tagIds).stream()
                .filter(tag -> tag.getDeletedAt() == null)
                .filter(tag -> tag.getType() == TagType.PHILOSOPHER || tag.getType() == TagType.VALUE)
                .forEach(tag -> battleOptionTagRepository.save(
                        BattleOptionTag.builder().battleOption(option).tag(tag).build()));
    }

    private void replaceBattleOptionTags(BattleOption option, List<Long> tagIds) {
        if (tagIds == null) return;

        List<BattleOptionTag> existingTags = battleOptionTagRepository.findByBattleOption(option);
        if (!existingTags.isEmpty()) {
            battleOptionTagRepository.deleteAll(existingTags);
        }

        saveBattleOptionTags(option, tagIds.stream().distinct().toList());
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

    private String resolveStoredImageKey(String rawReference, BattleStatus targetStatus, FileCategory fallbackCategory) {
        String normalized = normalizeStoredImageReference(rawReference, fallbackCategory);
        if (normalized == null) {
            return null;
        }
        if (targetStatus == BattleStatus.PUBLISHED && localDraftFileStorageService.isLocalDraftReference(normalized)) {
            return localDraftFileStorageService.promoteLocalDraftToS3(normalized, fallbackCategory, s3UploadService);
        }
        return normalized;
    }

    private String normalizeStoredImageReference(String rawReference, FileCategory fallbackCategory) {
        if (rawReference == null || rawReference.isBlank()) {
            return null;
        }

        String trimmed = rawReference.trim();
        String localNormalized = localDraftFileStorageService.normalizeLocalDraftKey(trimmed);
        if (localDraftFileStorageService.isLocalDraftReference(localNormalized)) {
            return localNormalized;
        }

        String path = extractPath(trimmed);
        Matcher matcher = RESOURCE_IMAGE_PATH_PATTERN.matcher(path);
        if (matcher.find()) {
            String categoryName = matcher.group(1);
            String fileName = matcher.group(2);
            try {
                FileCategory category = FileCategory.valueOf(categoryName);
                return category.getPath() + "/" + fileName;
            } catch (IllegalArgumentException ignored) {
                if (fallbackCategory != null) {
                    return fallbackCategory.getPath() + "/" + fileName;
                }
            }
        }

        return trimmed;
    }

    private String extractPath(String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            try {
                URI uri = URI.create(value);
                return uri.getPath();
            } catch (IllegalArgumentException ignored) {
                return value;
            }
        }
        return value;
    }

    private void deleteStoredAsset(String rawReference) {
        String normalized = normalizeStoredImageReference(rawReference, null);
        if (normalized == null) {
            return;
        }

        if (localDraftFileStorageService.isLocalDraftReference(normalized)) {
            localDraftFileStorageService.deleteIfLocalReference(normalized);
            return;
        }

        s3UploadService.deleteFile(normalized);
    }

    private BattleStatus parseBattleStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }

        try {
            return BattleStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private void validateBattleOptionCount(List<AdminBattleOptionRequest> options) {
        if (options == null) {
            throw new CustomException(ErrorCode.BATTLE_INVALID_OPTION_COUNT);
        }
        int count = options.size();
        if (count < 2 || count > 4) {
            throw new CustomException(ErrorCode.BATTLE_INVALID_OPTION_COUNT);
        }
    }
}



