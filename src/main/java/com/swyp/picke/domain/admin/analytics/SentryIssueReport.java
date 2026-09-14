package com.swyp.picke.domain.admin.analytics;

import java.time.Instant;
import java.util.List;

/**
 * Sentry 미해결 이슈 상위 목록.
 *
 * @param totalEvents 목록에 담긴 이슈들의 이벤트 합계. 프로젝트 전체 합계가 아니다.
 * @param issues      이벤트 수 내림차순.
 */
public record SentryIssueReport(
        AnalyticsStatus status,
        Instant fetchedAt,
        Long totalEvents,
        List<Issue> issues) {

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
}
