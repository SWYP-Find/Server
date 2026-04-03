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
        QuizVote v = saveOrUpdate(battleId, userId, request.optionId());
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
        QuizVote v = saveOrUpdate(battleId, userId, request.optionId());
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return quizVoteRepository.findByBattleAndUser(battle, user)
                .map(v -> {
                    long totalCount = quizVoteRepository.countByBattle(battle);
                    return new QuizVoteResponse(
                            battleId,
                            v.getSelectedOption().getId(),
                            totalCount,
                            calcStats(battle, totalCount)
                    );
                })
                .orElse(null);
    }

    @Override
    public PollVoteResponse getMyPollVote(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return quizVoteRepository.findByBattleAndUser(battle, user)
                .map(v -> {
                    long totalCount = quizVoteRepository.countByBattle(battle);
                    return new PollVoteResponse(
                            battleId,
                            v.getSelectedOption().getId(),
                            totalCount,
                            calcStats(battle, totalCount).stream()
                                    .map(s -> new PollVoteResponse.OptionStat(s.optionId(), s.label(), s.title(), s.voteCount(), s.ratio()))
                                    .toList()
                    );
                })
                .orElse(null);
    }

    private QuizVote saveOrUpdate(Long battleId, Long userId, Long optionId) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        BattleOption option = battleOptionRepository.findById(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        return quizVoteRepository.findByBattleAndUser(battle, user)
                .map(v -> { v.updateOption(option); return v; })
                .orElseGet(() -> {
                    battle.addParticipant();
                    return quizVoteRepository.save(
                            QuizVote.builder().user(user).battle(battle).selectedOption(option).build());
                });
    }

    private List<QuizVoteResponse.OptionStat> calcStats(Battle battle, long totalCount) {
        return battleOptionRepository.findByBattle(battle).stream().map(o -> {
            long count = quizVoteRepository.countByBattleAndSelectedOption(battle, o);
            double ratio = totalCount == 0 ? 0.0 : Math.round((double) count / totalCount * 1000) / 10.0;
            return new QuizVoteResponse.OptionStat(
                    o.getId(),
                    o.getLabel().name(),
                    o.getTitle(),
                    o.getIsCorrect(),
                    count,
                    ratio
            );
        }).toList();
    }
}