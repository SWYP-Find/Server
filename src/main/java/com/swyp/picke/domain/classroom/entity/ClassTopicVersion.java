package com.swyp.picke.domain.classroom.entity;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.classroom.enums.ClassTopicVersionSource;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 검증을 통과한 주제 버전 (PRD §5 topic_version_id). 클래스 개설에는 이 행만 쓸 수 있고, 만든 뒤에는 바꾸지 않는다.
 * <ul>
 *   <li>후보 선택(CANDIDATE)은 AI 추가 호출 없이, 직접 수정(EDITED)은 AI 의미 검증 통과 후에만 생성</li>
 *   <li>선택지 A/B 마다 관점을 대변할 철학자를 서버 허용 목록(PhilosopherType)에서 매핑해 둔다 (AI-02 토론에서 사용)</li>
 *   <li>source_ref : 후보 선택이면 "b{batch}:c{candidate}", 수정이면 바탕이 된 버전 "v{version_no}"</li>
 * </ul>
 */
@Getter
@Entity
@Table(
        name = "class_topic_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_topic_versions_draft_version", columnNames = {"draft_id", "version_no"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassTopicVersion extends BaseEntity {

    public static final int QUESTION_MAX_LENGTH = 60;
    public static final int DESCRIPTION_MAX_LENGTH = 80;
    public static final int OPTION_LABEL_MAX_LENGTH = 20;
    public static final int OPTION_RATIONALE_MAX_LENGTH = 40;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id", nullable = false)
    private ClassTopicDraft draft;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassTopicVersionSource source;

    @Column(name = "source_ref", length = 20)
    private String sourceRef;

    @Column(nullable = false, length = QUESTION_MAX_LENGTH)
    private String question;

    @Column(nullable = false, length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BattleCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BattleDifficulty difficulty;

    @Column(name = "option_a_label", nullable = false, length = OPTION_LABEL_MAX_LENGTH)
    private String optionALabel;

    @Column(name = "option_a_rationale", nullable = false, length = OPTION_RATIONALE_MAX_LENGTH)
    private String optionARationale;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_a_philosopher", nullable = false, length = 20)
    private PhilosopherType optionAPhilosopher;

    @Column(name = "option_b_label", nullable = false, length = OPTION_LABEL_MAX_LENGTH)
    private String optionBLabel;

    @Column(name = "option_b_rationale", nullable = false, length = OPTION_RATIONALE_MAX_LENGTH)
    private String optionBRationale;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_b_philosopher", nullable = false, length = 20)
    private PhilosopherType optionBPhilosopher;

    @Column(name = "profile_version", nullable = false, length = 30)
    private String profileVersion;

    @Column(name = "prompt_version", length = 30)
    private String promptVersion;

    @Builder
    private ClassTopicVersion(ClassTopicDraft draft, int versionNo, ClassTopicVersionSource source, String sourceRef,
                              String question, String description, BattleCategory category, BattleDifficulty difficulty,
                              String optionALabel, String optionARationale, PhilosopherType optionAPhilosopher,
                              String optionBLabel, String optionBRationale, PhilosopherType optionBPhilosopher,
                              String profileVersion, String promptVersion) {
        this.draft = draft;
        this.versionNo = versionNo;
        this.source = source;
        this.sourceRef = sourceRef;
        this.question = question;
        this.description = description;
        this.category = category;
        this.difficulty = difficulty;
        this.optionALabel = optionALabel;
        this.optionARationale = optionARationale;
        this.optionAPhilosopher = optionAPhilosopher;
        this.optionBLabel = optionBLabel;
        this.optionBRationale = optionBRationale;
        this.optionBPhilosopher = optionBPhilosopher;
        this.profileVersion = profileVersion;
        this.promptVersion = promptVersion;
    }
}
