package com.swyp.picke.domain.admin.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Sentry 프로젝트의 미해결 이슈를 이벤트 수 내림차순으로 읽는다.
 *
 * <p>인증은 조직 인증 토큰(Bearer)이다. 토큰이 없으면 조회하지 않고 NOT_CONFIGURED 를 준다.
 * 기간을 절대 날짜로 넘기려면 statsPeriod 를 빈 값으로 함께 보내야 한다.
 */
@Slf4j
@Component
public class SentryClient {

    private static final int ISSUE_LIMIT = 20;

    private final String baseUrl;
    private final String organization;
    private final List<String> projects;
    private final String authToken;
    private final AnalyticsHttpTransport transport;
    private final ObjectMapper objectMapper;

    @Autowired
    public SentryClient(
            @Value("${picke.analytics.sentry.base-url:https://sentry.io}") String baseUrl,
            @Value("${picke.analytics.sentry.organization:${SENTRY_ORG:picke}}") String organization,
            @Value("${picke.analytics.sentry.projects:${SENTRY_PROJECTS:picke-ios,picke-android}}")
            List<String> projects,
            @Value("${picke.analytics.sentry.auth-token:${SENTRY_AUTH_TOKEN:}}") String authToken,
            AnalyticsHttpTransport transport) {
        this(baseUrl, organization, projects, authToken, transport, new ObjectMapper());
    }

    SentryClient(String baseUrl, String organization, List<String> projects, String authToken,
                 AnalyticsHttpTransport transport, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.organization = organization;
        this.projects = projects;
        this.authToken = authToken;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    public SentryIssueReport fetchUnresolvedIssues(LocalDate from, LocalDate to) {
        if (!isConfigured()) {
            return SentryIssueReport.empty(AnalyticsStatus.NOT_CONFIGURED, from, to);
        }

        List<SentryIssueReport.ProjectIssues> fetched = projects.stream()
                .map(project -> fetchProject(project, from, to))
                .toList();
        boolean allConnected = fetched.stream()
                .allMatch(project -> project.status() == AnalyticsStatus.CONNECTED);
        Long total = allConnected
                ? fetched.stream().mapToLong(SentryIssueReport.ProjectIssues::totalEvents).sum()
                : null;
        return new SentryIssueReport(
                allConnected ? AnalyticsStatus.CONNECTED : AnalyticsStatus.UNAVAILABLE,
                Instant.now(), from, to, total, fetched);
    }

    /** 한 프로젝트가 막혀도 다른 프로젝트는 살린다. iOS 가 죽었다고 Android 지표까지 감추지 않는다. */
    private SentryIssueReport.ProjectIssues fetchProject(String project, LocalDate from, LocalDate to) {
        AnalyticsHttpResponse issueResponse = transport.get(issueUri(project, from, to),
                Map.of("Authorization", "Bearer " + authToken));
        AnalyticsHttpResponse statsResponse = transport.get(statsUri(project, from, to),
                Map.of("Authorization", "Bearer " + authToken));
        if (!issueResponse.isSuccess() || !issueResponse.isJson()
                || !statsResponse.isSuccess() || !statsResponse.isJson()) {
            log.warn("[Sentry] 조회 실패: project={}, issuesStatus={}, statsStatus={}",
                    project, issueResponse.statusCode(), statsResponse.statusCode());
            return SentryIssueReport.emptyProject(project, AnalyticsStatus.UNAVAILABLE);
        }

        try {
            List<SentryIssueReport.Issue> issues = parseIssues(issueResponse.body());
            List<SentryIssueReport.Day> days = parseDays(statsResponse.body(), from, to);
            long unresolvedTotal = issues.stream()
                    .map(SentryIssueReport.Issue::events)
                    .filter(events -> events != null)
                    .mapToLong(Long::longValue)
                    .sum();
            long total = days.stream().mapToLong(SentryIssueReport.Day::events).sum();
            return new SentryIssueReport.ProjectIssues(
                    project, AnalyticsStatus.CONNECTED, total, unresolvedTotal, days, issues);
        } catch (Exception e) {
            log.warn("[Sentry] 이슈 응답 파싱 실패: project={}, {}", project, e.getClass().getSimpleName());
            return SentryIssueReport.emptyProject(project, AnalyticsStatus.UNAVAILABLE);
        }
    }

    private boolean isConfigured() {
        return StringUtils.hasText(authToken)
                && StringUtils.hasText(organization)
                && projects != null
                && !projects.isEmpty();
    }

    private URI issueUri(String project, LocalDate from, LocalDate to) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/0/projects/{organization}/{project}/issues/")
                .queryParam("query", "is:unresolved")
                .queryParam("sort", "freq")
                .queryParam("limit", ISSUE_LIMIT)
                // 절대 기간을 쓰려면 statsPeriod 를 빈 값으로 넘겨야 한다. 생략하면 기본 기간이 적용된다.
                .queryParam("statsPeriod", "")
                .queryParam("start", from.atStartOfDay())
                .queryParam("end", to.atTime(LocalTime.MAX).withNano(0))
                .queryParam("utc", "true")
                .buildAndExpand(organization, project)
                .toUri();
    }

    private URI statsUri(String project, LocalDate from, LocalDate to) {
        long since = from.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long until = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/0/projects/{organization}/{project}/stats/")
                .queryParam("stat", "received")
                .queryParam("since", since)
                .queryParam("until", until)
                .queryParam("resolution", "1d")
                .buildAndExpand(organization, project)
                .toUri();
    }

    private List<SentryIssueReport.Day> parseDays(String body, LocalDate from, LocalDate to) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (!root.isArray()) {
            throw new IllegalArgumentException("Sentry stats response must be an array.");
        }
        Map<LocalDate, Long> counts = new java.util.HashMap<>();
        for (JsonNode point : root) {
            if (!point.isArray() || point.size() < 2 || !point.get(0).isNumber() || !point.get(1).isNumber()) {
                throw new IllegalArgumentException("Sentry stats point must contain timestamp and count.");
            }
            LocalDate date = Instant.ofEpochSecond(point.get(0).asLong()).atZone(ZoneOffset.UTC).toLocalDate();
            if (!date.isBefore(from) && !date.isAfter(to)) {
                counts.merge(date, point.get(1).asLong(), Long::sum);
            }
        }
        List<SentryIssueReport.Day> days = new ArrayList<>();
        for (LocalDate cursor = from; !cursor.isAfter(to); cursor = cursor.plusDays(1)) {
            days.add(new SentryIssueReport.Day(cursor, counts.getOrDefault(cursor, 0L)));
        }
        return days;
    }

    private List<SentryIssueReport.Issue> parseIssues(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (!root.isArray()) {
            throw new IllegalArgumentException("Sentry issue response must be an array.");
        }

        List<SentryIssueReport.Issue> parsed = new ArrayList<>();
        for (JsonNode node : root) {
            parsed.add(new SentryIssueReport.Issue(
                    text(node, "id"),
                    text(node, "title"),
                    text(node, "culprit"),
                    text(node, "level"),
                    number(node, "count"),
                    number(node, "userCount"),
                    instant(node, "firstSeen"),
                    instant(node, "lastSeen"),
                    text(node, "permalink")));
        }
        // sort=freq 는 절대 기간에서 이벤트 수 내림차순을 보장하지 않는다. 응답 순서를 믿지 않고 다시 정렬한다.
        return parsed.stream()
                .sorted(Comparator.comparing(SentryIssueReport.Issue::events,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /** Sentry 는 이벤트 수를 문자열로 준다. 숫자로 와도 읽히게 둔다. */
    private Long number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.isNumber() ? value.asLong() : Long.parseLong(value.asText());
    }

    private Instant instant(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? null : Instant.parse(value);
    }
}
