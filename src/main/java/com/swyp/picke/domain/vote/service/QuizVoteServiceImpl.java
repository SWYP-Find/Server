package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.vote.dto.request.QuizVoteRequest;
import com.swyp.picke.domain.vote.dto.response.PollVoteResponse;
import com.swyp.picke.domain.vote.dto.response.QuizVoteResponse;
import com.swyp.picke.domain.vote.entity.QuizVote;
import com.swyp.picke.domain.vote.repository.QuizVoteRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizVoteServiceImpl implements QuizVoteService {

    private final QuizVoteRepository quizVoteRepository;
    private final BattleService battleService;
    private final BattleOptionRepository battleOptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public QuizVoteResponse submitQuiz(Long battleId, Long userId, QuizVoteRequest request) {
        Battle battle = battleService.findById(battleId);
        if (!"QUIZ".equals(battle.getType().name())) {
            throw new CustomException(ErrorCode.BATTLE_NOT_QUIZ);
        }

        QuizVote v = saveOrUpdate(battle, userId, request.optionId());
        long totalCount = quizVoteRepository.countByBattle(v.getBattle());

        return new QuizVoteResponse(
                battleId,
                v.getSelectedOption().getId(),
                totalCount,
                calcStats(v.getBattle(), totalCount)
        );
    }

    @Override
    @Transactional
    public PollVoteResponse submitPoll(Long battleId, Long userId, QuizVoteRequest request) {
        Battle battle = battleService.findById(battleId);
        if (!"VOTE".equals(battle.getType().name())) {
            throw new CustomException(ErrorCode.BATTLE_NOT_POLL);
        }

        QuizVote v = saveOrUpdate(battle, userId, request.optionId());
        long totalCount = quizVoteRepository.countByBattle(v.getBattle());

        return new PollVoteResponse(
                battleId,
                v.getSelectedOption().getId(),
                totalCount,
                calcStats(v.getBattle(), totalCount).stream()
                        .map(s -> new PollVoteResponse.OptionStat(s.optionId(), s.label(), s.title(), s.voteCount(), s.ratio()))
                        .toList()
        );
    }

    @Override
    public QuizVoteResponse getMyQuizVote(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        if (!"QUIZ".equals(battle.getType().name())) {
            throw new CustomException(ErrorCode.BATTLE_NOT_QUIZ);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        long totalCount = quizVoteRepository.countByBattle(battle);

        return quizVoteRepository.findByBattleAndUser(battle, user)
                .map(v -> new QuizVoteResponse(
                        battleId,
                        v.getSelectedOption().getId(),
                        totalCount,
                        calcStats(battle, totalCount)
                ))
                .orElseGet(() -> {
                    // [투표 전] 전체 참여자 수(totalCount), 선택지 설명(stance)는 보여주되, 개별 통계(voteCount, ratio)는 0으로 숨김
                    List<QuizVoteResponse.OptionStat> blindStats = battleOptionRepository.findByBattle(battle).stream()
                            .map(o -> new QuizVoteResponse.OptionStat(
                                    o.getId(), o.getLabel().name(), o.getTitle(),
                                    o.getIsCorrect(), 0L, 0.0, o.getStance()
                            ))
                            .toList();
                    return new QuizVoteResponse(battleId, null, totalCount, blindStats);
                });
    }

    @Override
    public PollVoteResponse getMyPollVote(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        if (!"VOTE".equals(battle.getType().name())) {
            throw new CustomException(ErrorCode.BATTLE_NOT_POLL);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        long totalCount = quizVoteRepository.countByBattle(battle);

        return quizVoteRepository.findByBattleAndUser(battle, user)
                .map(v -> {
                    List<PollVoteResponse.OptionStat> stats = calcStats(battle, totalCount).stream()
                            .map(s -> new PollVoteResponse.OptionStat(s.optionId(), s.label(), s.title(), s.voteCount(), s.ratio()))
                            .toList();

                    return new PollVoteResponse(
                            battleId,
                            v.getSelectedOption().getId(),
                            totalCount,
                            stats
                    );
                })
                .orElseGet(() -> {
                    // [투표 전] 전체 참여자 수(totalCount)는 보여주되, 개별 통계(voteCount, ratio)는 0으로 숨김
                    List<PollVoteResponse.OptionStat> blindStats = battleOptionRepository.findByBattle(battle).stream()
                            .map(o -> new PollVoteResponse.OptionStat(o.getId(), o.getLabel().name(), o.getTitle(), 0L, 0.0))
                            .toList();
                    return new PollVoteResponse(battleId, null, totalCount, blindStats);
                });
    }

    @Transactional
    public void deleteQuizVoteByBattleId(Long battleId) {
        // 배틀 확인
        Battle battle = battleService.findById(battleId);

        // 해당 배틀의 모든 투표 조회
        List<QuizVote> votes = quizVoteRepository.findAllByBattle(battle);

        // 투표수 감소 (배틀 옵션에 반영)
        for (QuizVote v : votes) {
            if (v.getSelectedOption() != null) {
                v.getSelectedOption().decreaseVoteCount();
            }
        }
        quizVoteRepository.deleteAllInBatch(votes);
    }

    private QuizVote saveOrUpdate(Battle battle, Long userId, Long optionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        BattleOption newOption = battleOptionRepository.findById(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        return quizVoteRepository.findByBattleAndUser(battle, user)
                .map(v -> {
                    // 옵션을 바꾼다면 기존 옵션 -1, 새 옵션 +1
                    if (!v.getSelectedOption().equals(newOption)) {
                        v.getSelectedOption().decreaseVoteCount();
                        newOption.increaseVoteCount();
                        v.updateOption(newOption);
                    }
                    return v;
                })
                .orElseGet(() -> {
                    // 처음 투표한다면 새 옵션 +1
                    battle.addParticipant();
                    newOption.increaseVoteCount();
                    return quizVoteRepository.save(
                            QuizVote.builder().user(user).battle(battle).selectedOption(newOption).build());
                });
        }

    private List<QuizVoteResponse.OptionStat> calcStats(Battle battle, long totalCount) {
        return battleOptionRepository.findByBattle(battle).stream().map(o -> {
            long count = (o.getVoteCount() == null) ? 0L : o.getVoteCount();
            double ratio = totalCount == 0 ? 0.0 : Math.round((double) count / totalCount * 1000) / 10.0;
            return new QuizVoteResponse.OptionStat(
                    o.getId(),
                    o.getLabel().name(),
                    o.getTitle(),
                    o.getIsCorrect(),
                    count,
                    ratio,
                    null
            );
        }).toList();
    }
}