package com.swyp.picke.domain.quiz.repository;

import com.swyp.picke.domain.quiz.entity.QuizOption;
import com.swyp.picke.domain.quiz.entity.QuizOptionValueTagMap;
import com.swyp.picke.domain.quiz.entity.QuizOptionValueTagMapId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizOptionValueTagMapRepository extends JpaRepository<QuizOptionValueTagMap, QuizOptionValueTagMapId> {
    List<QuizOptionValueTagMap> findByQuizOption(QuizOption quizOption);
    void deleteByQuizOption(QuizOption quizOption);
}

