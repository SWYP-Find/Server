package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.entity.QuizOption;
import com.swyp.picke.domain.quiz.repository.QuizOptionRepository;
import com.swyp.picke.domain.quiz.service.QuizService;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.vote.dto.request.QuizVoteRequest;
import com.swyp.picke.domain.vote.dto.response.QuizVoteResponse;
import com.swyp.picke.domain.vote.entity.QuizVote;
import com.swyp.picke.domain.vote.repository.QuizVoteRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizVoteServiceImpl implements QuizVoteService {

    private final QuizService quizService;
    private final QuizOptionRepository quizOptionRepository;
    private final QuizVoteRepository quizVoteRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public QuizVoteResponse submitQuiz(Long battleId, Long userId, QuizVoteRequest request) {
        Long quizId = battleId;
        Quiz quiz = quizService.findById(quizId);

        QuizOption selectedOption = quizOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        if (!selectedOption.getQuiz().getId().equals(quiz.getId())) {
            throw new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND);
        }

        QuizVote quizVote = saveOrUpdate(quiz, userId, selectedOption);
        long totalCount = quiz.getTotalParticipantsCount() == null ? 0L : quiz.getTotalParticipantsCount();

        return new QuizVoteResponse(
                quizId,
                quizVote.getSelectedOption().getId(),
                totalCount,
                buildStats(quiz, totalCount, true, true)
        );
    }

    @Override
    public QuizVoteResponse getMyQuizVote(Long battleId, Long userId) {
        Long quizId = battleId;
        Quiz quiz = quizService.findById(quizId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        long totalCount = quiz.getTotalParticipantsCount() == null ? 0L : quiz.getTotalParticipantsCount();

        return quizVoteRepository.findByQuizAndUser(quiz, user)
                .map(quizVote -> new QuizVoteResponse(
                        quizId,
                        quizVote.getSelectedOption().getId(),
                        totalCount,
                        buildStats(quiz, totalCount, true, true)
                ))
                .orElseGet(() -> new QuizVoteResponse(
                        quizId,
                        null,
                        totalCount,
                        buildStats(quiz, totalCount, false, false)
                ));
    }

    @Override
    @Transactional
    public void deleteQuizVoteByBattleId(Long battleId) {
        Long quizId = battleId;
        Quiz quiz = quizService.findById(quizId);

        List<QuizVote> votes = quizVoteRepository.findAllByQuiz(quiz);
        for (QuizVote ignored : votes) {
            quiz.decreaseTotalParticipantsCount();
        }
        quizVoteRepository.deleteAllInBatch(votes);
    }

    private QuizVote saveOrUpdate(Quiz quiz, Long userId, QuizOption selectedOption) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return quizVoteRepository.findByQuizAndUser(quiz, user)
                .map(quizVote -> {
                    if (!quizVote.getSelectedOption().equals(selectedOption)) {
                        quizVote.updateOption(selectedOption);
                    }
                    return quizVote;
                })
                .orElseGet(() -> {
                    quiz.increaseTotalParticipantsCount();
                    return quizVoteRepository.save(
                            QuizVote.builder()
                                    .user(user)
                                    .quiz(quiz)
                                    .selectedOption(selectedOption)
                                    .build()
                    );
                });
    }

    private List<QuizVoteResponse.OptionStat> buildStats(
            Quiz quiz,
            long totalCount,
            boolean revealCorrect,
            boolean revealCounts
    ) {
        return quizOptionRepository.findByQuizOrderByDisplayOrderAscLabelAscIdAsc(quiz).stream()
                .map(option -> {
                    long voteCount = revealCounts
                            ? quizVoteRepository.countByQuizAndSelectedOption(quiz, option)
                            : 0L;

                    double ratio = (!revealCounts || totalCount == 0)
                            ? 0.0
                            : Math.round((double) voteCount / totalCount * 1000) / 10.0;

                    return new QuizVoteResponse.OptionStat(
                            option.getId(),
                            option.getLabel().name(),
                            option.getText(),
                            revealCorrect ? option.getIsCorrect() : null,
                            voteCount,
                            ratio,
                            option.getDetailText()
                    );
                })
                .toList();
    }
}