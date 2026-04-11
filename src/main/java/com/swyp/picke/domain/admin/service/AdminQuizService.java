package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.quiz.request.AdminQuizCreateRequest;
import com.swyp.picke.domain.admin.dto.quiz.request.AdminQuizUpdateRequest;
import com.swyp.picke.domain.admin.dto.quiz.response.AdminQuizDeleteResponse;
import com.swyp.picke.domain.admin.dto.quiz.response.AdminQuizDetailResponse;
import com.swyp.picke.domain.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminQuizService {

    private final QuizService quizService;

    public AdminQuizDetailResponse createQuiz(AdminQuizCreateRequest request) {
        return quizService.createQuiz(request);
    }

    public AdminQuizDetailResponse getQuizDetail(Long quizId) {
        return quizService.getAdminQuizDetail(quizId);
    }

    public AdminQuizDetailResponse updateQuiz(Long quizId, AdminQuizUpdateRequest request) {
        return quizService.updateQuiz(quizId, request);
    }

    public AdminQuizDeleteResponse deleteQuiz(Long quizId) {
        return quizService.deleteQuiz(quizId);
    }

    public Object getQuizzes(int page, int size, String status) {
        return quizService.getQuizzes(page, size);
    }
}