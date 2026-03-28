package com.swyp.app.domain.recommendation.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.entity.BattleOptionTag;
import com.swyp.app.domain.battle.repository.BattleOptionRepository;
import com.swyp.app.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.app.domain.battle.repository.BattleRepository;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.domain.recommendation.dto.response.RecommendationListResponse;
import com.swyp.app.domain.tag.enums.TagType;
import com.swyp.app.domain.user.entity.PhilosopherType;
import com.swyp.app.domain.user.service.UserService;
import com.swyp.app.domain.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private static final int SAME_TYPE_COUNT = 3;
    private static final int OPPOSITE_TYPE_COUNT = 2;

    private final BattleService battleService;
    private final BattleRepository battleRepository;
    private final BattleOptionRepository battleOptionRepository;
    private final BattleOptionTagRepository battleOptionTagRepository;
    private final VoteRepository voteRepository;
    private final UserService userService;

    public RecommendationListResponse getInterestingBattles(Long battleId, Long userId) {
        battleService.findById(battleId);

        // 현재 유저의 철학자 유형 및 반대 유형
        PhilosopherType myType = userService.getPhilosopherType(userId);
        PhilosopherType oppositeType = myType.getWorstMatch();

        // 현재 유저가 이미 참여한 배틀 ID 목록 (제외 대상)
        List<Long> excludeBattleIds = voteRepository.findParticipatedBattleIdsByUserId(userId);
        if (excludeBattleIds.isEmpty()) excludeBattleIds = List.of(-1L);

        // TODO: 철학자 유형별 유저 ID 조회 로직 구현 필요
        //  - PhilosopherType별 누적 점수 테이블이 구현되면 아래 TODO를 대체
        List<Long> sameTypeUserIds = findUserIdsByPhilosopherType(myType);
        List<Long> oppositeTypeUserIds = findUserIdsByPhilosopherType(oppositeType);

        // 같은 유형 유저들이 참여한 배틀 후보 ID
        List<Long> sameCandidateIds = sameTypeUserIds.isEmpty()
                ? List.of()
                : voteRepository.findParticipatedBattleIdsByUserIds(sameTypeUserIds);

        // 반대 유형 유저들이 참여한 배틀 후보 ID
        List<Long> oppositeCandidateIds = oppositeTypeUserIds.isEmpty()
                ? List.of()
                : voteRepository.findParticipatedBattleIdsByUserIds(oppositeTypeUserIds);

        // 인기 점수 기준 배틀 조회 (Score = V*1.0 + C*1.5 + Vw*0.2)
        // 철학자 유형 로직 미구현 시 인기 배틀로 폴백
        List<Battle> sameBattles = sameCandidateIds.isEmpty()
                ? battleRepository.findPopularBattlesExcluding(excludeBattleIds, PageRequest.of(0, SAME_TYPE_COUNT))
                : battleRepository.findRecommendedBattles(sameCandidateIds, excludeBattleIds, PageRequest.of(0, SAME_TYPE_COUNT));

        List<Battle> oppositeBattles = oppositeCandidateIds.isEmpty()
                ? battleRepository.findPopularBattlesExcluding(excludeBattleIds, PageRequest.of(0, OPPOSITE_TYPE_COUNT))
                : battleRepository.findRecommendedBattles(oppositeCandidateIds, excludeBattleIds, PageRequest.of(0, OPPOSITE_TYPE_COUNT));

        List<Battle> result = new ArrayList<>();
        result.addAll(sameBattles);
        result.addAll(oppositeBattles);

        List<RecommendationListResponse.Item> items = result.stream()
                .map(this::toItem)
                .collect(Collectors.toList());

        return new RecommendationListResponse(items, null, false);
    }

    private RecommendationListResponse.Item toItem(Battle battle) {
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);

        List<RecommendationListResponse.OptionSummary> optionSummaries = options.stream()
                .map(opt -> new RecommendationListResponse.OptionSummary(
                        opt.getId(),
                        opt.getLabel().name(),
                        opt.getTitle(),
                        opt.getStance(),
                        opt.getRepresentative(),
                        opt.getImageUrl()
                ))
                .toList();

        // CATEGORY 태그만 노출
        List<RecommendationListResponse.TagSummary> tagSummaries = options.stream()
                .flatMap(opt -> battleOptionTagRepository.findByBattleOption(opt).stream())
                .map(BattleOptionTag::getTag)
                .filter(tag -> tag.getType() == TagType.CATEGORY)
                .distinct()
                .map(tag -> new RecommendationListResponse.TagSummary(tag.getId(), tag.getName()))
                .toList();

        return new RecommendationListResponse.Item(
                battle.getId(),
                battle.getTitle(),
                battle.getSummary(),
                battle.getAudioDuration(),
                battle.getViewCount(),
                tagSummaries,
                battle.getTotalParticipantsCount(),
                optionSummaries
        );
    }

    /**
     * TODO: 철학자 유형별 유저 ID 조회 구현 필요
     * - 사후투표 시 BattleOptionTag(PHILOSOPHER 타입) 기반으로 유저별 철학자 점수 누적 테이블 구현 후 대체
     * - 현재는 빈 리스트 반환
     */
    private List<Long> findUserIdsByPhilosopherType(PhilosopherType type) {
        return List.of();
    }
}
