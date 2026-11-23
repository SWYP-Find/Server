package com.swyp.picke.domain.admin.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 클래스 운영 대시보드에 필요한 Sentry 이슈와 일별 발생 수만 담는다. */
public record SentryIssueSummaryReport(
        AnalyticsStatus status,
        Instant fetchedAt,
        LocalDate from,
        LocalDate to,
        Long totalEvents,
        List<ProjectSummary> projects) {

    public record ProjectSummary(
            String project,
            AnalyticsStatus status,
            Long totalEvents,
            List<SentryIssueReport.Issue> issues) {
    }

    static SentryIssueSummaryReport empty(AnalyticsStatus status, LocalDate from, LocalDate to) {
        return new SentryIssueSummaryReport(status, null, from, to, null, List.of());
    }

    static ProjectSummary emptyProject(String project) {
        return new ProjectSummary(project, AnalyticsStatus.UNAVAILABLE, null, List.of());
    }
}
