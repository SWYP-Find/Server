package com.swyp.picke.domain.quiz.entity;

import com.swyp.picke.domain.quiz.enums.QuizStatus;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "quizzes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quiz extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "total_participants_count", nullable = false)
    private Long totalParticipantsCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizStatus status;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<QuizOption> options = new ArrayList<>();

    @Builder
    public Quiz(String title, LocalDate targetDate, QuizStatus status) {
        this.title = title;
        this.targetDate = targetDate;
        this.status = status;
        this.totalParticipantsCount = 0L;
    }

    public void update(String title, LocalDate targetDate, QuizStatus status) {
        if (title != null) this.title = title;
        if (targetDate != null) this.targetDate = targetDate;
        if (status != null) this.status = status;
    }

    public void increaseTotalParticipantsCount() {
        this.totalParticipantsCount = (this.totalParticipantsCount == null ? 0L : this.totalParticipantsCount) + 1L;
    }

    public void decreaseTotalParticipantsCount() {
        long current = this.totalParticipantsCount == null ? 0L : this.totalParticipantsCount;
        this.totalParticipantsCount = Math.max(0L, current - 1L);
    }
}
