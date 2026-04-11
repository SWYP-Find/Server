package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.entity.QuizOption;
import com.swyp.picke.domain.quiz.enums.QuizOptionLabel;
import com.swyp.picke.domain.quiz.enums.QuizStatus;
import com.swyp.picke.domain.quiz.repository.QuizOptionRepository;
import com.swyp.picke.domain.quiz.service.QuizService;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.vote.dto.request.QuizVoteRequest;
import com.swyp.picke.domain.vote.dto.response.QuizVoteResponse;
import com.swyp.picke.domain.vote.entity.QuizVote;
import com.swyp.picke.domain.vote.repository.QuizVoteRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizVoteServiceImplTest {

    @Mock
    private QuizService quizService;

    @Mock
    private QuizOptionRepository quizOptionRepository;

    @Mock
    private QuizVoteRepository quizVoteRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuizVoteServiceImpl quizVoteService;

    @Test
    @DisplayName("퀴즈 신규 투표 시 totalParticipantsCount가 증가한다")
    void submitQuiz_increases_totalParticipants_on_new_vote() {
        Long quizId = 1L;
        Long userId = 10L;
        Long optionId = 101L;

        Quiz quiz = Quiz.builder()
                .title("퀴즈")
                .targetDate(LocalDate.now())
                .status(QuizStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(quiz, "id", quizId);

        QuizOption optionA = QuizOption.builder()
                .quiz(quiz)
                .label(QuizOptionLabel.A)
                .text("A")
                .detailText("설명")
                .isCorrect(true)
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(optionA, "id", optionId);

        User user = org.mockito.Mockito.mock(User.class);

        when(quizService.findById(quizId)).thenReturn(quiz);
        when(quizOptionRepository.findById(optionId)).thenReturn(Optional.of(optionA));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(quizVoteRepository.findByQuizAndUser(quiz, user)).thenReturn(Optional.empty());
        when(quizVoteRepository.save(any(QuizVote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quizOptionRepository.findByQuizOrderByDisplayOrderAscLabelAscIdAsc(quiz)).thenReturn(List.of(optionA));
        when(quizVoteRepository.countByQuizAndSelectedOption(quiz, optionA)).thenReturn(1L);

        QuizVoteResponse response = quizVoteService.submitQuiz(quizId, userId, new QuizVoteRequest(optionId));

        assertThat(quiz.getTotalParticipantsCount()).isEqualTo(1L);
        assertThat(response.totalCount()).isEqualTo(1L);
        assertThat(response.selectedOptionId()).isEqualTo(optionId);
    }
}