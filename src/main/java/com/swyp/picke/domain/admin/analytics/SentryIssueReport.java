package com.swyp.picke.domain.admin.analytics;

import java.time.Instant;
import java.util.List;

/**
 * Sentry 미해결 이슈 상위 목록. iOS·Android 를 프로젝트별로 나눠 담는다.
 *
 * @param status      프로젝트 전부가 연결됐을 때만 CONNECTED.
 * @param totalEvents 전부 연결됐을 때의 프로젝트 합계. 하나라도 못 읽으면 null 이다.
 *                    일부만 더한 값을 전체 합계처럼 보여주지 않는다.
 */
public record SentryIssueReport(
        AnalyticsStatus status,
        Instant fetchedAt,
        Long totalEvents,
        List<ProjectIssues> projects) {

    /**
     * @param project     Sentry 프로젝트 슬러그.
     * @param totalEvents 이 프로젝트 목록에 담긴 이슈들의 합계. 프로젝트 전체 이벤트 수가 아니다.
     * @param issues      이벤트 수 내림차순.
     */
    public record ProjectIssues(
            String project,
            AnalyticsStatus status,
            Long totalEvents,
            List<Issue> issues) {
    }

    /**
     * @param events   기간 내 이벤트 수.
     * @param users    영향받은 사용자 수. Sentry가 주지 않으면 null.
     * @param level    error, fatal, warning 등.
     * @param lastSeen 마지막 발생 시각.
     */
    public record Issue(
            String id,
            String title,
            String culprit,
            String level,
            Long events,
            Long users,
            Instant firstSeen,
            Instant lastSeen,
            String permalink) {
    }

    static SentryIssueReport empty(AnalyticsStatus status) {
        return new SentryIssueReport(status, null, null, List.of());
    }

    static ProjectIssues emptyProject(String project, AnalyticsStatus status) {
        return new ProjectIssues(project, status, null, List.of());
    }
}
