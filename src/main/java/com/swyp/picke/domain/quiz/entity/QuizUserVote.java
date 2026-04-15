package com.swyp.picke.domain.quiz.entity;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Entity;
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
@Table(name = "quiz_user_votes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizUserVote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private QuizOption selectedOption;

    @Builder
    public QuizUserVote(User user, Quiz quiz, QuizOption selectedOption) {
        this.user = user;
        this.quiz = quiz;
        this.selectedOption = selectedOption;
    }

    public void updateOption(QuizOption option) {
        this.selectedOption = option;
    }
}
