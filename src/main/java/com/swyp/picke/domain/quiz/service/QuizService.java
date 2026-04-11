package com.swyp.picke.domain.quiz.service;

import com.swyp.picke.domain.admin.dto.quiz.request.AdminQuizCreateRequest;
import com.swyp.picke.domain.admin.dto.quiz.request.AdminQuizUpdateRequest;
import com.swyp.picke.domain.admin.dto.quiz.response.AdminQuizDeleteResponse;
import com.swyp.picke.domain.admin.dto.quiz.response.AdminQuizDetailResponse;
import com.swyp.picke.domain.quiz.dto.response.QuizDetailResponse;
import com.swyp.picke.domain.quiz.dto.response.QuizListResponse;
import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.entity.QuizOption;
import java.util.List;

public interface QuizService {
    Quiz findById(Long quizId);

    QuizListResponse getQuizzes(int page, int size);

    List<Quiz> getTodayPicks(int limit);

    List<QuizOption> getOptions(Quiz quiz);

    long countVotes(Quiz quiz);

    QuizDetailResponse getQuizDetail(Long quizId);

    AdminQuizDetailResponse getAdminQuizDetail(Long quizId);

    AdminQuizDetailResponse createQuiz(AdminQuizCreateRequest request);

    AdminQuizDetailResponse updateQuiz(Long quizId, AdminQuizUpdateRequest request);

    AdminQuizDeleteResponse deleteQuiz(Long quizId);
}


