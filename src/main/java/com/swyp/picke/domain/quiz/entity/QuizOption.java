package com.swyp.picke.domain.quiz.entity;

import com.swyp.picke.domain.quiz.enums.QuizOptionLabel;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "quiz_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QuizOptionLabel label;

    @Column(nullable = false, length = 300)
    private String text;

    @Column(name = "detail_text", length = 1000)
    private String detailText;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Builder
    public QuizOption(
            Quiz quiz,
            QuizOptionLabel label,
            String text,
            String detailText,
            Boolean isCorrect,
            Integer displayOrder
    ) {
        this.quiz = quiz;
        this.label = label;
        this.text = text;
        this.detailText = detailText;
        this.isCorrect = (isCorrect != null) ? isCorrect : false;
        this.displayOrder = displayOrder;
    }

    void assignQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public void update(String text, String detailText, Boolean isCorrect) {
        if (text != null) this.text = text;
        if (detailText != null) this.detailText = detailText;
        if (isCorrect != null) this.isCorrect = isCorrect;
        if (displayOrder != null) this.displayOrder = displayOrder;
    }
}
