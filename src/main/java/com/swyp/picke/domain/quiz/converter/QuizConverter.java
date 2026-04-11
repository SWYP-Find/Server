package com.swyp.picke.domain.quiz.converter;

import com.swyp.picke.domain.admin.dto.quiz.request.AdminQuizCreateRequest;
import com.swyp.picke.domain.admin.dto.quiz.response.AdminQuizDetailResponse;
import com.swyp.picke.domain.quiz.dto.response.QuizDetailResponse;
import com.swyp.picke.domain.quiz.dto.response.QuizListResponse;
import com.swyp.picke.domain.quiz.dto.response.QuizOptionResponse;
import com.swyp.picke.domain.quiz.dto.response.QuizSimpleResponse;
import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.entity.QuizOption;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class QuizConverter {

    private static final Comparator<QuizOption> OPTION_SORTER =
            Comparator.comparing((QuizOption option) -> option.getDisplayOrder() == null ? Integer.MAX_VALUE : option.getDisplayOrder())
                    .thenComparing(option -> option.getLabel() == null ? "" : option.getLabel().name())
                    .thenComparing(QuizOption::getId);

    public Quiz toEntity(AdminQuizCreateRequest request) {
        return Quiz.builder()
                .title(request.title())
                .status(request.status())
                .build();
    }

    public QuizListResponse toListResponse(Page<Quiz> quizPage) {
        List<QuizSimpleResponse> items = quizPage.getContent().stream()
                .map(this::toSimpleResponse)
                .toList();
        return new QuizListResponse(items, quizPage.getNumber() + 1, quizPage.getTotalPages(), quizPage.getTotalElements());
    }

    public QuizSimpleResponse toSimpleResponse(Quiz quiz) {
        return new QuizSimpleResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getStatus(),
                quiz.getCreatedAt()
        );
    }

    public AdminQuizDetailResponse toAdminDetailResponse(Quiz quiz, List<QuizOption> options) {
        return new AdminQuizDetailResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getTargetDate(),
                quiz.getStatus(),
                toOptionResponses(options)
        );
    }

    public QuizDetailResponse toDetailResponse(Quiz quiz, List<QuizOption> options) {
        return new QuizDetailResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getTargetDate(),
                quiz.getStatus(),
                toOptionResponses(options),
                quiz.getCreatedAt(),
                quiz.getUpdatedAt()
        );
    }

    private List<QuizOptionResponse> toOptionResponses(List<QuizOption> options) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .sorted(OPTION_SORTER)
                .map(option -> new QuizOptionResponse(
                        option.getId(),
                        option.getLabel(),
                        option.getText(),
                        option.getDetailText(),
                        option.getIsCorrect(),
                        option.getDisplayOrder()
                ))
                .toList();
    }
}
