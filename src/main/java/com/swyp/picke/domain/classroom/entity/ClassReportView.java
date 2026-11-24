package com.swyp.picke.domain.classroom.entity;

import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리포트 열람 기록 (#689). 사용자·리포트당 1행에 첫·마지막 열람 시각과 횟수만 둔다.
 * 성과 분석의 리포트 열람률과 Mixpanel class_report_view 의 기준이다.
 * 클래스 원문 파기(ClassDataPurgeService) 때 함께 지운다. 외래키 없이 ID 만 둔다.
 */
@Getter
@Entity
@Table(name = "class_report_views",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_report_views_report_user", columnNames = {"report_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassReportView extends BaseEntity {

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "first_viewed_at", nullable = false)
    private LocalDateTime firstViewedAt;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    public ClassReportView(Long classId, Long reportId, Long userId, LocalDateTime now) {
        this.classId = classId;
        this.reportId = reportId;
        this.userId = userId;
        this.firstViewedAt = now;
        this.lastViewedAt = now;
        this.viewCount = 1;
    }

    public void viewAgain(LocalDateTime now) {
        this.lastViewedAt = now;
        this.viewCount++;
    }
}
