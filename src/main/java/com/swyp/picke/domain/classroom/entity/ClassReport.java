package com.swyp.picke.domain.classroom.entity;

import com.swyp.picke.domain.classroom.enums.ClassReportStatus;
import com.swyp.picke.domain.classroom.enums.ClassReportType;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * GPT 생성 결과 캐시. 클래스 마감 후 1회 생성한다 (D06).
 * <ul>
 *   <li>CLASS_SUMMARY 는 member null, MEMBER_FEEDBACK 은 member 필수</li>
 *   <li>UK(class_id, member_id, type) 는 member null 포함을 위해 SQL 의 NULLS NOT DISTINCT 인덱스로 관리</li>
 *   <li>content(MEMBER_FEEDBACK) : headline, judgment_basis, strength, next_focus, next_question,
 *       evidence_refs[], analysis_status, coverage</li>
 * </ul>
 */
@Getter
@Entity
@Table(name = "class_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private ClassMember member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassReportType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassReportStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content")
    private Map<String, Object> content;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_job_id")
    private AiJob aiJob;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Builder
    private ClassReport(Classroom classroom, ClassMember member, ClassReportType type) {
        this.classroom = classroom;
        this.member = member;
        this.type = type;
        this.status = ClassReportStatus.PENDING;
    }

    public void startGenerating(AiJob aiJob) {
        this.status = ClassReportStatus.GENERATING;
        this.aiJob = aiJob;
    }

    public void complete(Map<String, Object> content, LocalDateTime now) {
        this.status = ClassReportStatus.SUCCEEDED;
        this.content = content;
        this.generatedAt = now;
    }

    /** 작성 글이 없어 서버 템플릿으로 대체. AI 호출 없음. */
    public void limit(Map<String, Object> templateContent, LocalDateTime now) {
        this.status = ClassReportStatus.LIMITED;
        this.content = templateContent;
        this.generatedAt = now;
    }

    public void fail() {
        this.status = ClassReportStatus.FAILED;
    }

    /** 마감 스냅샷(정량 통계) 저장. 아직 AI 결과는 없으므로 상태는 PENDING 그대로 (PRD §14 "같은 종료 스냅샷") */
    public void attachSnapshot(Map<String, Object> snapshot) {
        this.content = snapshot;
    }

    /** 운영자 재시도: FAILED 를 PENDING 으로 되돌려 스케줄러가 같은 job 으로 다시 처리하게 한다 */
    public void requeue() {
        this.status = ClassReportStatus.PENDING;
    }

    public boolean isDone() {
        return status == ClassReportStatus.SUCCEEDED || status == ClassReportStatus.LIMITED;
    }
}
