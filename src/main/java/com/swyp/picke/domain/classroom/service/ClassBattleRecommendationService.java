package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.classroom.dto.response.ClassBattleRecommendationResponse;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.util.ResourceUrlProvider;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 새 클래스 만들기 > Picke 배틀 > 조건에 맞는 콘텐츠 추천. 생성형 AI 없이 검색·필터로만 고른다 (PRD "기존 콘텐츠 추천은 검색·필터 기능").
 * <ul>
 *   <li>후보: 공개·미삭제·클래스 추천이 켜진 배틀 중 수준이 같고 카테고리가 하나라도 겹치는 배틀</li>
 *   <li>주제를 적으면 쉼표·공백으로 나눈 단어가 제목·요약·설명에 하나 이상 들어간 배틀만 남기고, 많이 들어간 순으로 정렬</li>
 *   <li>최대 3개. 조건에 맞는 배틀이 적으면 채우지 않고 적은 대로 돌려준다</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassBattleRecommendationService {

    static final int MAX_RESULTS = 3;
    static final int MAX_CATEGORIES = 3;
    static final int KEYWORD_MAX_LENGTH = 50;
    static final int MAX_KEYWORD_TOKENS = 5;
    private static final Pattern KEYWORD_SEPARATOR = Pattern.compile("[,\\s]+");

    private final BattleRepository battleRepository;
    private final BattleOptionRepository battleOptionRepository;
    private final ResourceUrlProvider urlProvider;

    public ClassBattleRecommendationResponse recommend(String keyword, BattleDifficulty difficulty, List<String> categories) {
        if (difficulty == null) {
            throw new CustomException(ErrorCode.CLASS_RECOMMEND_INVALID_CONDITION);
        }
        Set<BattleCategory> categorySet = parseCategories(categories);
        List<String> tokens = parseKeyword(keyword);

        List<Battle> picked = battleRepository
                .findClassRecommendCandidates(BattleStatus.PUBLISHED, difficulty, categorySet).stream()
                .map(battle -> new Scored(battle, matchCount(battle, tokens)))
                .filter(scored -> tokens.isEmpty() || scored.matches() > 0)
                .sorted(Comparator.comparingInt(Scored::matches).reversed()
                        .thenComparing(scored -> Boolean.TRUE.equals(scored.battle().getIsEditorPick()), Comparator.reverseOrder())
                        .thenComparing(scored -> participantsOf(scored.battle()), Comparator.reverseOrder())
                        .thenComparing(scored -> scored.battle().getId(), Comparator.reverseOrder()))
                .limit(MAX_RESULTS)
                .map(Scored::battle)
                .toList();

        Map<Long, List<BattleOption>> optionsByBattle = picked.isEmpty() ? Map.of()
                : battleOptionRepository.findByBattleIn(picked).stream()
                        .collect(Collectors.groupingBy(option -> option.getBattle().getId()));
        return new ClassBattleRecommendationResponse(picked.stream()
                .map(battle -> toItem(battle, optionsByBattle.getOrDefault(battle.getId(), List.of())))
                .toList());
    }

    // ===== 조건 =====

    /** 한글 이름(철학) 또는 enum 이름(PHILOSOPHY). 중복 제거 후 1~3개 */
    static Set<BattleCategory> parseCategories(List<String> raw) {
        Set<BattleCategory> result = new LinkedHashSet<>();
        if (raw != null) {
            for (String value : raw) {
                result.add(toCategory(value));
            }
        }
        if (result.isEmpty() || result.size() > MAX_CATEGORIES) {
            throw new CustomException(ErrorCode.CLASS_RECOMMEND_INVALID_CONDITION);
        }
        return result;
    }

    private static BattleCategory toCategory(String value) {
        String trimmed = value == null ? "" : value.strip();
        BattleCategory byLabel = BattleCategory.from(trimmed);
        if (byLabel != null) {
            return byLabel;
        }
        return Arrays.stream(BattleCategory.values())
                .filter(category -> category.name().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_RECOMMEND_INVALID_CONDITION));
    }

    /** 비어 있으면 주제 조건 없음. 50자 초과면 400. 쉼표·공백으로 나눠 앞 5개 단어만 쓴다 */
    static List<String> parseKeyword(String keyword) {
        String text = keyword == null ? "" : keyword.strip();
        if (text.length() > KEYWORD_MAX_LENGTH) {
            throw new CustomException(ErrorCode.CLASS_RECOMMEND_INVALID_CONDITION);
        }
        return KEYWORD_SEPARATOR.splitAsStream(text)
                .filter(token -> !token.isEmpty())
                .map(token -> token.toLowerCase(Locale.ROOT))
                .distinct()
                .limit(MAX_KEYWORD_TOKENS)
                .toList();
    }

    private static int matchCount(Battle battle, List<String> tokens) {
        if (tokens.isEmpty()) {
            return 0;
        }
        String text = String.join(" ",
                Objects.toString(battle.getTitle(), ""),
                Objects.toString(battle.getSummary(), ""),
                Objects.toString(battle.getDescription(), "")).toLowerCase(Locale.ROOT);
        return (int) tokens.stream().filter(text::contains).count();
    }

    private static long participantsOf(Battle battle) {
        return battle.getTotalParticipantsCount() == null ? 0L : battle.getTotalParticipantsCount();
    }

    // ===== 응답 =====

    private ClassBattleRecommendationResponse.Item toItem(Battle battle, List<BattleOption> options) {
        return new ClassBattleRecommendationResponse.Item(
                battle.getId(),
                battle.getTitle(),
                battle.getSummary(),
                urlProvider.getImageUrl(FileCategory.BATTLE, battle.getThumbnailUrl()),
                battle.getCategory(),
                battle.getDifficulty(),
                battle.getAudioDuration(),
                options.stream()
                        .sorted(Comparator.comparing(BattleOption::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toOption)
                        .toList());
    }

    private ClassBattleRecommendationResponse.Option toOption(BattleOption option) {
        Integer order = option.getDisplayOrder();
        return new ClassBattleRecommendationResponse.Option(
                option.getId(),
                order == null ? null : (order == 1 ? "A" : "B"),
                option.getTitle(),
                option.getStance(),
                option.getRepresentative(),
                urlProvider.getImageUrl(FileCategory.PHILOSOPHER, option.getImageUrl()));
    }

    private record Scored(Battle battle, int matches) {
    }
}
