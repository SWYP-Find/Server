package com.swyp.picke.domain.vote.repository;

import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.entity.QuizOption;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.vote.entity.QuizVote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizVoteRepository extends JpaRepository<QuizVote, Long> {
    Optional<QuizVote> findByQuizAndUser(Quiz quiz, User user);
    List<QuizVote> findAllByQuiz(Quiz quiz);
    long countByQuizAndSelectedOption(Quiz quiz, QuizOption selectedOption);
}
