package com.swyp.picke.domain.quiz.repository;

import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.entity.QuizOption;
import com.swyp.picke.domain.quiz.entity.QuizUserVote;
import com.swyp.picke.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizUserVoteRepository extends JpaRepository<QuizUserVote, Long> {
    Optional<QuizUserVote> findByQuizAndUser(Quiz quiz, User user);
    long countByQuiz(Quiz quiz);
    long countByQuizAndSelectedOption(Quiz quiz, QuizOption selectedOption);
    List<QuizUserVote> findAllByQuiz(Quiz quiz);
}
