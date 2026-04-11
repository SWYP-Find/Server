package com.swyp.picke.domain.quiz.repository;

import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.entity.QuizOption;
import com.swyp.picke.domain.quiz.enums.QuizOptionLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizOptionRepository extends JpaRepository<QuizOption, Long> {
    List<QuizOption> findByQuizOrderByDisplayOrderAscLabelAscIdAsc(Quiz quiz);
    Optional<QuizOption> findByQuizAndLabel(Quiz quiz, QuizOptionLabel label);
}
