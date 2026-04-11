package com.swyp.picke.domain.quiz.repository;

import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.entity.QuizTagMap;
import com.swyp.picke.domain.quiz.entity.QuizTagMapId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizTagMapRepository extends JpaRepository<QuizTagMap, QuizTagMapId> {
    List<QuizTagMap> findByQuiz(Quiz quiz);
    void deleteByQuiz(Quiz quiz);
}

