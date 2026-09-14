package com.swyp.picke.domain.admin.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
        LocalDate from,
        LocalDate to,
        Long totalEvents,
        List<ProjectIssues> projects) {

    /**
     * @param project     Sentry 프로젝트 슬러그.
     * @param totalEvents 프로젝트 stats API가 반환한 선택 기간 전체 수신 이벤트 수.
     * @param issues      이벤트 수 내림차순.
     */
    public record ProjectIssues(
            String project,
            AnalyticsStatus status,
            Long totalEvents,
            Long unresolvedEvents,
            List<Day> days,
            List<Issue> issues,
            List<Event> recentEvents,
            List<DatasetSeries> datasets,
            MetricCatalog metricCatalog,
            SessionHealth sessionHealth,
            ResourceCatalog releases) {
    }

    public record Day(LocalDate date, Long events) {
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

    /**
     * Sentry 프로젝트 오류 이벤트 목록 API가 full=true 에서 주는 진단 필드다.
     * details 는 Sentry 응답 전체를 보존해 SDK 컨텍스트가 추가돼도 서버 계약에서 유실되지 않게 한다.
     */
    public record Event(
            String eventId,
            String id,
            String groupId,
            String projectId,
            String title,
            String message,
            String platform,
            String type,
            String location,
            String culprit,
            String crashFile,
            Instant dateCreated,
            List<Tag> tags,
            Map<String, Object> metadata,
            Map<String, Object> details) {
    }

    public record Tag(String key, String value) {
    }

    /** errors, logs, spans, profile_functions, tracemetrics 데이터셋의 일별 발생량. */
    public record DatasetSeries(
            String dataset,
            AnalyticsStatus status,
            Long total,
            List<Day> days) {
    }

    /** 앱이 전송한 커스텀 Sentry 메트릭의 이름·타입·단위·건수·마지막 수집 시각 원본. */
    public record MetricCatalog(
            AnalyticsStatus status,
            List<Map<String, Object>> entries) {
    }

    public record SessionHealth(
            AnalyticsStatus status,
            List<SessionSeries> series,
            Map<String, Object> details) {
    }

    public record SessionSeries(
            String status,
            Long total,
            List<Day> days) {
    }

    public record ResourceCatalog(
            AnalyticsStatus status,
            List<Map<String, Object>> entries) {
    }

    static SentryIssueReport empty(AnalyticsStatus status, LocalDate from, LocalDate to) {
        return new SentryIssueReport(status, null, from, to, null, List.of());
    }

    static ProjectIssues emptyProject(String project, AnalyticsStatus status) {
        return new ProjectIssues(project, status, null, null, List.of(), List.of(), List.of(), List.of(),
                new MetricCatalog(status, List.of()),
                new SessionHealth(status, List.of(), Map.of()),
                new ResourceCatalog(status, List.of()));
    }
}
