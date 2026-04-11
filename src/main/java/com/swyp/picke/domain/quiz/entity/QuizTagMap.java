package com.swyp.picke.domain.quiz.entity;

import com.swyp.picke.domain.tag.entity.CategoryTag;
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
@Table(name = "quiz_tags")
@IdClass(QuizTagMapId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizTagMap {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_tag_id", nullable = false)
    private CategoryTag categoryTag;

    @Builder
    public QuizTagMap(Quiz quiz, CategoryTag categoryTag) {
        this.quiz = quiz;
        this.categoryTag = categoryTag;
    }
}

