package com.swyp.picke.domain.admin.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
    private final String project;
    private final String authToken;
    private final AnalyticsHttpTransport transport;
    private final ObjectMapper objectMapper;

    @Autowired
    public SentryClient(
            @Value("${picke.analytics.sentry.base-url:https://sentry.io}") String baseUrl,
            @Value("${picke.analytics.sentry.organization:${SENTRY_ORG:}}") String organization,
            @Value("${picke.analytics.sentry.project:${SENTRY_PROJECT:}}") String project,
            @Value("${picke.analytics.sentry.auth-token:${SENTRY_AUTH_TOKEN:}}") String authToken,
            AnalyticsHttpTransport transport) {
        this(baseUrl, organization, project, authToken, transport, new ObjectMapper());
    }

    SentryClient(String baseUrl, String organization, String project, String authToken,
                 AnalyticsHttpTransport transport, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.organization = organization;
        this.project = project;
        this.authToken = authToken;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    public SentryIssueReport fetchUnresolvedIssues(LocalDate from, LocalDate to) {
        if (!isConfigured()) {
            return SentryIssueReport.empty(AnalyticsStatus.NOT_CONFIGURED);
        }

        AnalyticsHttpResponse response = transport.get(uri(from, to),
                Map.of("Authorization", "Bearer " + authToken));
        if (!response.isSuccess() || !response.isJson()) {
            log.warn("[Sentry] 이슈 조회 실패: status={}", response.statusCode());
            return SentryIssueReport.empty(AnalyticsStatus.UNAVAILABLE);
        }

        try {
            List<SentryIssueReport.Issue> issues = parseIssues(response.body());
            long total = issues.stream()
                    .map(SentryIssueReport.Issue::events)
                    .filter(events -> events != null)
                    .mapToLong(Long::longValue)
                    .sum();
            return new SentryIssueReport(AnalyticsStatus.CONNECTED, Instant.now(), total, issues);
        } catch (Exception e) {
            log.warn("[Sentry] 이슈 응답 파싱 실패: {}", e.getClass().getSimpleName());
            return SentryIssueReport.empty(AnalyticsStatus.UNAVAILABLE);
        }
    }

    private boolean isConfigured() {
        return StringUtils.hasText(authToken)
                && StringUtils.hasText(organization)
                && StringUtils.hasText(project);
    }

    private URI uri(LocalDate from, LocalDate to) {
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

    private List<SentryIssueReport.Issue> parseIssues(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (!root.isArray()) {
            throw new IllegalArgumentException("Sentry issue response must be an array.");
        }

        List<SentryIssueReport.Issue> issues = new ArrayList<>();
        for (JsonNode node : root) {
            issues.add(new SentryIssueReport.Issue(
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
        return issues;
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
