package com.swyp.picke.domain.quiz.service;

import com.swyp.picke.domain.quiz.converter.QuizConverter;
import com.swyp.picke.domain.admin.dto.quiz.request.AdminQuizCreateRequest;
import com.swyp.picke.domain.admin.dto.quiz.request.AdminQuizOptionRequest;
import com.swyp.picke.domain.admin.dto.quiz.request.AdminQuizUpdateRequest;
import com.swyp.picke.domain.admin.dto.quiz.response.AdminQuizDeleteResponse;
import com.swyp.picke.domain.admin.dto.quiz.response.AdminQuizDetailResponse;
import com.swyp.picke.domain.quiz.dto.response.QuizDetailResponse;
import com.swyp.picke.domain.quiz.dto.response.QuizListResponse;
import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.entity.QuizOption;
import com.swyp.picke.domain.quiz.enums.QuizOptionLabel;
import com.swyp.picke.domain.quiz.enums.QuizStatus;
import com.swyp.picke.domain.quiz.repository.QuizOptionRepository;
import com.swyp.picke.domain.quiz.repository.QuizRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final QuizConverter quizConverter;

    @Override
    public Quiz findById(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));
    }

    @Override
    public QuizListResponse getQuizzes(int page, int size) {
        int pageNumber = Math.max(0, page - 1);
        Page<Quiz> quizPage = quizRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(pageNumber, size));
        return quizConverter.toListResponse(quizPage);
    }

    @Override
    @Transactional
    public List<Quiz> getTodayPicks(int limit) {
        int safeLimit = Math.max(1, limit);
        LocalDate today = LocalDate.now();

        ensureTodayPicks(today, safeLimit);
        return quizRepository.findTodayPicks(QuizStatus.PUBLISHED, today, PageRequest.of(0, safeLimit));
    }

    @Override
    public List<QuizOption> getOptions(Quiz quiz) {
        return quizOptionRepository.findByQuizOrderByDisplayOrderAscLabelAscIdAsc(quiz);
    }

    @Override
    public long countVotes(Quiz quiz) {
        return quiz.getTotalParticipantsCount() == null ? 0L : quiz.getTotalParticipantsCount();
    }

    @Override
    public QuizDetailResponse getQuizDetail(Long quizId) {
        Quiz quiz = findById(quizId);
        List<QuizOption> options = quizOptionRepository.findByQuizOrderByDisplayOrderAscLabelAscIdAsc(quiz);
        return quizConverter.toDetailResponse(quiz, options);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public AdminQuizDetailResponse getAdminQuizDetail(Long quizId) {
        Quiz quiz = findById(quizId);
        List<QuizOption> options = quizOptionRepository.findByQuizOrderByDisplayOrderAscLabelAscIdAsc(quiz);
        return quizConverter.toAdminDetailResponse(quiz, options);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminQuizDetailResponse createQuiz(AdminQuizCreateRequest request) {
        Quiz quiz = quizConverter.toEntity(request);
        quiz = quizRepository.save(quiz);

        List<QuizOption> savedOptions = new ArrayList<>();
        if (request.options() != null) {
            for (int i = 0; i < request.options().size(); i++) {
                AdminQuizOptionRequest optionRequest = request.options().get(i);
                int displayOrder = resolveDisplayOrder(optionRequest.displayOrder(), i + 1);
                QuizOption option = QuizOption.builder()
                        .quiz(quiz)
                        .label(optionRequest.label())
                        .text(optionRequest.text())
                        .detailText(optionRequest.detailText())
                        .isCorrect(optionRequest.isCorrect())
                        .displayOrder(displayOrder)
                        .build();
                option = quizOptionRepository.save(option);
                savedOptions.add(option);
            }
        }

        return quizConverter.toAdminDetailResponse(quiz, savedOptions);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminQuizDetailResponse updateQuiz(Long quizId, AdminQuizUpdateRequest request) {
        Quiz quiz = findById(quizId);
        quiz.update(request.title(), request.targetDate(), request.status());

        if (request.options() != null) {
            List<QuizOption> existingOptions = quizOptionRepository.findByQuizOrderByDisplayOrderAscLabelAscIdAsc(quiz);
            Map<QuizOptionLabel, QuizOption> existingOptionMap = new HashMap<>();
            for (QuizOption option : existingOptions) {
                existingOptionMap.put(option.getLabel(), option);
            }

            Set<QuizOptionLabel> requestedLabels = new HashSet<>();
            for (int i = 0; i < request.options().size(); i++) {
                AdminQuizOptionRequest optionRequest = request.options().get(i);
                int displayOrder = resolveDisplayOrder(optionRequest.displayOrder(), i + 1);
                requestedLabels.add(optionRequest.label());
                QuizOption option = existingOptionMap.get(optionRequest.label());

                if (option == null) {
                    option = QuizOption.builder()
                            .quiz(quiz)
                            .label(optionRequest.label())
                            .text(optionRequest.text())
                            .detailText(optionRequest.detailText())
                            .isCorrect(optionRequest.isCorrect())
                            .displayOrder(displayOrder)
                            .build();
                    option = quizOptionRepository.save(option);
                } else {
                    option.update(
                            optionRequest.text(),
                            optionRequest.detailText(),
                            optionRequest.isCorrect(),
                            displayOrder
                    );
                }
            }

            for (QuizOption existingOption : existingOptions) {
                if (requestedLabels.contains(existingOption.getLabel())) continue;
                quizOptionRepository.delete(existingOption);
            }
        }

        return getAdminQuizDetail(quizId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminQuizDeleteResponse deleteQuiz(Long quizId) {
        Quiz quiz = findById(quizId);
        List<QuizOption> options = quizOptionRepository.findByQuizOrderByDisplayOrderAscLabelAscIdAsc(quiz);
        quizOptionRepository.deleteAll(options);
        quizRepository.delete(quiz);
        return new AdminQuizDeleteResponse(true, LocalDateTime.now());
    }

    private int resolveDisplayOrder(Integer requestedOrder, int fallbackOrder) {
        if (requestedOrder == null || requestedOrder < 1) {
            return fallbackOrder;
        }
        return requestedOrder;
    }

    private void ensureTodayPicks(LocalDate today, int requiredCount) {
        List<Quiz> todays = quizRepository.findTodayPicks(QuizStatus.PUBLISHED, today, PageRequest.of(0, requiredCount));
        int missingCount = requiredCount - todays.size();
        if (missingCount <= 0) return;

        List<Quiz> candidates = quizRepository.findAutoAssignableTodayPicks(
                QuizStatus.PUBLISHED,
                today,
                PageRequest.of(0, missingCount)
        );
        for (Quiz candidate : candidates) {
            candidate.update(null, today, null);
        }
    }
}

