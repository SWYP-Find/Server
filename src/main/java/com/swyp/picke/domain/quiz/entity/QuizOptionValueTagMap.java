package com.swyp.picke.domain.quiz.entity;

import com.swyp.picke.domain.tag.entity.ValueTag;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "quiz_option_value_tags")
@IdClass(QuizOptionValueTagMapId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizOptionValueTagMap {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_option_id", nullable = false)
    private QuizOption quizOption;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_tag_id", nullable = false)
    private ValueTag valueTag;

    @Builder
    public QuizOptionValueTagMap(QuizOption quizOption, ValueTag valueTag) {
        this.quizOption = quizOption;
        this.valueTag = valueTag;
    }
}

