package com.swyp.picke.domain.admin.analytics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * Sentry 프로젝트의 오류·로그·성능·프로파일·메트릭·세션·릴리즈 데이터를 읽는다.
 *
 * <p>인증은 조직 인증 토큰(Bearer)이다. 토큰이 없으면 조회하지 않고 NOT_CONFIGURED 를 준다.
 * 기간을 절대 날짜로 넘기려면 statsPeriod 를 빈 값으로 함께 보내야 한다.
 */
@Slf4j
@Component
public class SentryClient {

    private static final int ISSUE_LIMIT = 20;
    /** full=true 는 Sentry가 페이지 크기를 최대 10건으로 제한한다. */
    private static final int RECENT_EVENT_LIMIT = 10;
    private static final int ANALYTICS_EVENT_BATCH_SIZE = 10;
    private static final String ANALYTICS_EVENT_FIELD = "tag[analytics_event,string]";
    private static final List<String> DATASETS = List.of(
            "errors", "logs", "spans", "profile_functions", "tracemetrics");

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
        AnalyticsHttpResponse eventResponse = transport.get(eventUri(project, from, to),
                Map.of("Authorization", "Bearer " + authToken));
        if (!issueResponse.isSuccess() || !issueResponse.isJson()
                || !statsResponse.isSuccess() || !statsResponse.isJson()
                || !eventResponse.isSuccess() || !eventResponse.isJson()) {
            log.warn("[Sentry] 조회 실패: project={}, issuesStatus={}, statsStatus={}, eventsStatus={}",
                    project, issueResponse.statusCode(), statsResponse.statusCode(), eventResponse.statusCode());
            return SentryIssueReport.emptyProject(project, AnalyticsStatus.UNAVAILABLE);
        }

        try {
            List<SentryIssueReport.Issue> issues = parseIssues(issueResponse.body());
            List<SentryIssueReport.Day> days = parseDays(statsResponse.body(), from, to);
            List<SentryIssueReport.Event> recentEvents = parseEvents(eventResponse.body());
            List<SentryIssueReport.DatasetSeries> datasets = new ArrayList<>(DATASETS.stream()
                    .map(dataset -> fetchDataset(project, dataset, from, to))
                    .toList());
            datasets.add(fetchSignUps(project, from, to));
            SentryIssueReport.AnalyticsEventCatalog analyticsEvents = fetchAnalyticsEvents(project, from, to);
            SentryIssueReport.MetricCatalog metricCatalog = fetchMetricCatalog(project, from, to);
            SentryIssueReport.SessionHealth sessionHealth = fetchSessionHealth(project, from, to);
            SentryIssueReport.ResourceCatalog releases = fetchReleases(project);
            long unresolvedTotal = issues.stream()
                    .map(SentryIssueReport.Issue::events)
                    .filter(events -> events != null)
                    .mapToLong(Long::longValue)
                    .sum();
            long total = days.stream().mapToLong(SentryIssueReport.Day::events).sum();
            return new SentryIssueReport.ProjectIssues(
                    project, AnalyticsStatus.CONNECTED, total, unresolvedTotal, days, issues, recentEvents,
                    datasets, analyticsEvents, metricCatalog, sessionHealth, releases);
        } catch (Exception e) {
            log.warn("[Sentry] 핵심 응답 파싱 실패: project={}, {}", project, e.getClass().getSimpleName());
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

    private URI eventUri(String project, LocalDate from, LocalDate to) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/0/projects/{organization}/{project}/events/")
                .queryParam("start", from.atStartOfDay())
                .queryParam("end", to.atTime(LocalTime.MAX).withNano(0))
                .queryParam("full", "true")
                .buildAndExpand(organization, project)
                .toUri();
    }

    private SentryIssueReport.DatasetSeries fetchDataset(
            String project, String dataset, LocalDate from, LocalDate to) {
        String yAxis = dataset.equals("tracemetrics") ? "sum(value)" : "count()";
        return fetchDataset(project, dataset, dataset, yAxis, null, from, to);
    }

    private SentryIssueReport.DatasetSeries fetchSignUps(
            String project, LocalDate from, LocalDate to) {
        return fetchDataset(project, "sign_up", "tracemetrics", "sum(value)",
                "metric.name:\"user.action.count\" event:sign_up", from, to);
    }

    private SentryIssueReport.DatasetSeries fetchDataset(
            String project,
            String responseName,
            String sentryDataset,
            String yAxis,
            String query,
            LocalDate from,
            LocalDate to) {
        AnalyticsHttpResponse response = transport.get(
                datasetUri(project, sentryDataset, yAxis, query, from, to),
                Map.of("Authorization", "Bearer " + authToken));
        if (!response.isSuccess() || !response.isJson()) {
            log.warn("[Sentry] 데이터셋 조회 실패: project={}, dataset={}, status={}",
                    project, responseName, response.statusCode());
            return new SentryIssueReport.DatasetSeries(
                    responseName, AnalyticsStatus.UNAVAILABLE, null, List.of());
        }
        try {
            List<SentryIssueReport.Day> days = parseDatasetDays(response.body(), from, to);
            long total = days.stream().mapToLong(SentryIssueReport.Day::events).sum();
            return new SentryIssueReport.DatasetSeries(responseName, AnalyticsStatus.CONNECTED, total, days);
        } catch (Exception e) {
            log.warn("[Sentry] 데이터셋 파싱 실패: project={}, dataset={}, {}",
                    project, responseName, e.getClass().getSimpleName());
            return new SentryIssueReport.DatasetSeries(
                    responseName, AnalyticsStatus.UNAVAILABLE, null, List.of());
        }
    }

    private URI datasetUri(
            String project,
            String dataset,
            String yAxis,
            String query,
            LocalDate from,
            LocalDate to) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/0/organizations/{organization}/events-timeseries/")
                .queryParam("project", project)
                .queryParam("dataset", dataset)
                .queryParam("start", from.atStartOfDay())
                .queryParam("end", to.atTime(LocalTime.MAX).withNano(0))
                .queryParam("interval", 86400)
                .queryParam("yAxis", yAxis);
        if (StringUtils.hasText(query)) {
            builder.queryParam("query", query);
        }
        return builder.buildAndExpand(organization).toUri();
    }

    private SentryIssueReport.MetricCatalog fetchMetricCatalog(String project, LocalDate from, LocalDate to) {
        AnalyticsHttpResponse response = transport.get(metricCatalogUri(project, from, to),
                Map.of("Authorization", "Bearer " + authToken));
        if (!response.isSuccess() || !response.isJson()) {
            return new SentryIssueReport.MetricCatalog(AnalyticsStatus.UNAVAILABLE, List.of());
        }
        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) {
                throw new IllegalArgumentException("Sentry metric catalog response must be an array.");
            }
            List<Map<String, Object>> entries = new ArrayList<>();
            for (JsonNode node : root) {
                entries.add(objectMapper.convertValue(node, new TypeReference<>() { }));
            }
            return new SentryIssueReport.MetricCatalog(AnalyticsStatus.CONNECTED, List.copyOf(entries));
        } catch (Exception e) {
            log.warn("[Sentry] 메트릭 카탈로그 파싱 실패: project={}, {}",
                    project, e.getClass().getSimpleName());
            return new SentryIssueReport.MetricCatalog(AnalyticsStatus.UNAVAILABLE, List.of());
        }
    }

    private URI metricCatalogUri(String project, LocalDate from, LocalDate to) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/0/organizations/{organization}/trace-items/metrics/")
                .queryParam("project", project)
                .queryParam("start", from.atStartOfDay())
                .queryParam("end", to.atTime(LocalTime.MAX).withNano(0))
                .queryParam("sort", "-count")
                .queryParam("expand", "context")
                .buildAndExpand(organization)
                .toUri();
    }

    private SentryIssueReport.AnalyticsEventCatalog fetchAnalyticsEvents(
            String project, LocalDate from, LocalDate to) {
        AnalyticsHttpResponse catalogResponse = transport.get(analyticsEventCatalogUri(project, from, to),
                Map.of("Authorization", "Bearer " + authToken));
        if (!catalogResponse.isSuccess() || !catalogResponse.isJson()) {
            log.warn("[Sentry] 분석 이벤트 목록 조회 실패: project={}, status={}",
                    project, catalogResponse.statusCode());
            return unavailableAnalyticsEvents();
        }

        try {
            List<SentryIssueReport.AnalyticsEventSeries> catalog = parseAnalyticsEventCatalog(
                    catalogResponse.body());
            if (catalog.isEmpty()) {
                return new SentryIssueReport.AnalyticsEventCatalog(
                        AnalyticsStatus.CONNECTED, 0L, List.of());
            }

            Map<String, List<SentryIssueReport.AnalyticsEventDay>> daysByEvent = new LinkedHashMap<>();
            List<String> names = catalog.stream().map(SentryIssueReport.AnalyticsEventSeries::event).toList();
            for (int start = 0; start < names.size(); start += ANALYTICS_EVENT_BATCH_SIZE) {
                List<String> batch = names.subList(start, Math.min(start + ANALYTICS_EVENT_BATCH_SIZE, names.size()));
                AnalyticsHttpResponse seriesResponse = transport.get(
                        analyticsEventTimeseriesUri(project, batch, from, to),
                        Map.of("Authorization", "Bearer " + authToken));
                if (!seriesResponse.isSuccess() || !seriesResponse.isJson()) {
                    log.warn("[Sentry] 분석 이벤트 추이 조회 실패: project={}, status={}",
                            project, seriesResponse.statusCode());
                    return unavailableAnalyticsEvents();
                }
                daysByEvent.putAll(parseAnalyticsEventDays(seriesResponse.body(), batch, from, to));
            }

            List<SentryIssueReport.AnalyticsEventSeries> events = catalog.stream()
                    .map(series -> new SentryIssueReport.AnalyticsEventSeries(
                            series.event(), series.total(), series.uniqueUsers(), series.firstSeen(), series.lastSeen(),
                            daysByEvent.getOrDefault(series.event(), emptyAnalyticsDays(from, to))))
                    .toList();
            long total = events.stream().map(SentryIssueReport.AnalyticsEventSeries::total)
                    .filter(value -> value != null).mapToLong(Long::longValue).sum();
            return new SentryIssueReport.AnalyticsEventCatalog(
                    AnalyticsStatus.CONNECTED, total, events);
        } catch (Exception e) {
            log.warn("[Sentry] 분석 이벤트 응답 파싱 실패: project={}, {}",
                    project, e.getClass().getSimpleName());
            return unavailableAnalyticsEvents();
        }
    }

    private URI analyticsEventCatalogUri(String project, LocalDate from, LocalDate to) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/0/organizations/{organization}/events/")
                .queryParam("project", project)
                .queryParam("dataset", "errors")
                .queryParam("start", from.atStartOfDay())
                .queryParam("end", to.atTime(LocalTime.MAX).withNano(0))
                .queryParam("field", ANALYTICS_EVENT_FIELD)
                .queryParam("field", "count()")
                .queryParam("field", "count_unique(user)")
                .queryParam("field", "min(timestamp)")
                .queryParam("field", "max(timestamp)")
                .queryParam("query", "has:analytics_event")
                .queryParam("sort", "-count()")
                .queryParam("per_page", 100)
                .buildAndExpand(organization)
                .toUri();
    }

    private URI analyticsEventTimeseriesUri(
            String project, List<String> events, LocalDate from, LocalDate to) {
        String query = events.stream()
                .map(event -> "analytics_event:\"" + event.replace("\"", "\\\"") + "\"")
                .reduce((left, right) -> left + " OR " + right)
                .map(value -> events.size() > 1 ? "(" + value + ")" : value)
                .orElse("has:analytics_event");
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/0/organizations/{organization}/events-timeseries/")
                .queryParam("project", project)
                .queryParam("dataset", "errors")
                .queryParam("start", from.atStartOfDay())
                .queryParam("end", to.atTime(LocalTime.MAX).withNano(0))
                .queryParam("interval", 86400)
                .queryParam("query", query)
                .queryParam("groupBy", ANALYTICS_EVENT_FIELD)
                .queryParam("topEvents", events.size())
                .queryParam("sort", "-count()")
                .queryParam("excludeOther", 1)
                .queryParam("yAxis", "count()")
                .queryParam("yAxis", "count_unique(user)")
                .buildAndExpand(organization)
                .toUri();
    }

    private List<SentryIssueReport.AnalyticsEventSeries> parseAnalyticsEventCatalog(String body) throws Exception {
        JsonNode data = objectMapper.readTree(body).path("data");
        if (!data.isArray()) {
            throw new IllegalArgumentException("Sentry analytics event response must contain data.");
        }
        List<SentryIssueReport.AnalyticsEventSeries> events = new ArrayList<>();
        for (JsonNode row : data) {
            String event = analyticsEventName(row);
            if (!StringUtils.hasText(event)) {
                continue;
            }
            events.add(new SentryIssueReport.AnalyticsEventSeries(
                    event,
                    number(row, "count()"),
                    number(row, "count_unique(user)"),
                    instant(row, "min(timestamp)"),
                    instant(row, "max(timestamp)"),
                    List.of()));
        }
        return events.stream()
                .sorted(Comparator.comparing(SentryIssueReport.AnalyticsEventSeries::total,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private Map<String, List<SentryIssueReport.AnalyticsEventDay>> parseAnalyticsEventDays(
            String body, List<String> events, LocalDate from, LocalDate to) throws Exception {
        JsonNode timeSeries = objectMapper.readTree(body).path("timeSeries");
        if (!timeSeries.isArray()) {
            throw new IllegalArgumentException("Sentry analytics timeseries response must contain timeSeries.");
        }
        Map<String, Map<LocalDate, Long>> counts = new LinkedHashMap<>();
        Map<String, Map<LocalDate, Long>> users = new LinkedHashMap<>();
        for (JsonNode series : timeSeries) {
            String event = analyticsEventNameFromGroup(series.path("groupBy"));
            if (!StringUtils.hasText(event) || !events.contains(event)) {
                continue;
            }
            Map<String, Map<LocalDate, Long>> target = "count_unique(user)".equals(text(series, "yAxis"))
                    ? users : counts;
            Map<LocalDate, Long> valuesByDate = target.computeIfAbsent(event, ignored -> new LinkedHashMap<>());
            JsonNode values = series.path("values");
            if (!values.isArray()) {
                continue;
            }
            for (JsonNode point : values) {
                LocalDate date = Instant.ofEpochMilli(point.path("timestamp").asLong())
                        .atZone(ZoneOffset.UTC).toLocalDate();
                if (!date.isBefore(from) && !date.isAfter(to) && point.path("value").isNumber()) {
                    valuesByDate.merge(date, point.path("value").asLong(), Long::sum);
                }
            }
        }

        Map<String, List<SentryIssueReport.AnalyticsEventDay>> result = new LinkedHashMap<>();
        for (String event : events) {
            List<SentryIssueReport.AnalyticsEventDay> days = new ArrayList<>();
            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                days.add(new SentryIssueReport.AnalyticsEventDay(
                        date,
                        counts.getOrDefault(event, Map.of()).getOrDefault(date, 0L),
                        users.getOrDefault(event, Map.of()).getOrDefault(date, 0L)));
            }
            result.put(event, List.copyOf(days));
        }
        return result;
    }

    private String analyticsEventName(JsonNode row) {
        for (String field : List.of(ANALYTICS_EVENT_FIELD, "tag[analytics_event]", "analytics_event")) {
            String value = text(row, field);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String analyticsEventNameFromGroup(JsonNode groupBy) {
        if (!groupBy.isArray()) {
            return null;
        }
        for (JsonNode group : groupBy) {
            String key = text(group, "key");
            if (ANALYTICS_EVENT_FIELD.equals(key) || "tag[analytics_event]".equals(key)
                    || "analytics_event".equals(key)) {
                return text(group, "value");
            }
        }
        return null;
    }

    private List<SentryIssueReport.AnalyticsEventDay> emptyAnalyticsDays(LocalDate from, LocalDate to) {
        List<SentryIssueReport.AnalyticsEventDay> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            days.add(new SentryIssueReport.AnalyticsEventDay(date, 0L, 0L));
        }
        return List.copyOf(days);
    }

    private SentryIssueReport.AnalyticsEventCatalog unavailableAnalyticsEvents() {
        return new SentryIssueReport.AnalyticsEventCatalog(
                AnalyticsStatus.UNAVAILABLE, null, List.of());
    }

    private SentryIssueReport.SessionHealth fetchSessionHealth(
            String project, LocalDate from, LocalDate to) {
        AnalyticsHttpResponse response = transport.get(sessionUri(project, from, to),
                Map.of("Authorization", "Bearer " + authToken));
        if (!response.isSuccess() || !response.isJson()) {
            return new SentryIssueReport.SessionHealth(AnalyticsStatus.UNAVAILABLE, List.of(), Map.of());
        }
        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode intervals = root.path("intervals");
            JsonNode groups = root.path("groups");
            if (!intervals.isArray() || !groups.isArray()) {
                throw new IllegalArgumentException("Sentry session response must contain intervals and groups.");
            }
            List<SentryIssueReport.SessionSeries> series = new ArrayList<>();
            for (JsonNode group : groups) {
                String status = text(group.path("by"), "session.status");
                Long total = number(group.path("totals"), "sum(session)");
                JsonNode values = group.path("series").path("sum(session)");
                List<SentryIssueReport.Day> days = new ArrayList<>();
                for (int index = 0; index < Math.min(intervals.size(), values.size()); index++) {
                    LocalDate date = Instant.parse(intervals.get(index).asText()).atZone(ZoneOffset.UTC).toLocalDate();
                    if (!date.isBefore(from) && !date.isAfter(to)) {
                        days.add(new SentryIssueReport.Day(date, values.get(index).asLong()));
                    }
                }
                series.add(new SentryIssueReport.SessionSeries(status, total, List.copyOf(days)));
            }
            Map<String, Object> details = objectMapper.convertValue(root, new TypeReference<>() { });
            return new SentryIssueReport.SessionHealth(
                    AnalyticsStatus.CONNECTED, List.copyOf(series), details);
        } catch (Exception e) {
            log.warn("[Sentry] 세션 상태 파싱 실패: project={}, {}", project, e.getClass().getSimpleName());
            return new SentryIssueReport.SessionHealth(AnalyticsStatus.UNAVAILABLE, List.of(), Map.of());
        }
    }

    private URI sessionUri(String project, LocalDate from, LocalDate to) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/0/organizations/{organization}/sessions/")
                .queryParam("project", project)
                .queryParam("field", "sum(session)")
                .queryParam("groupBy", "session.status")
                .queryParam("interval", "1d")
                .queryParam("start", from.atStartOfDay())
                .queryParam("end", to.atTime(LocalTime.MAX).withNano(0))
                .buildAndExpand(organization)
                .toUri();
    }

    private SentryIssueReport.ResourceCatalog fetchReleases(String project) {
        AnalyticsHttpResponse response = transport.get(releaseUri(project),
                Map.of("Authorization", "Bearer " + authToken));
        if (!response.isSuccess() || !response.isJson()) {
            return new SentryIssueReport.ResourceCatalog(AnalyticsStatus.UNAVAILABLE, List.of());
        }
        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) {
                throw new IllegalArgumentException("Sentry release response must be an array.");
            }
            List<Map<String, Object>> entries = new ArrayList<>();
            for (JsonNode node : root) {
                entries.add(objectMapper.convertValue(node, new TypeReference<>() { }));
            }
            return new SentryIssueReport.ResourceCatalog(AnalyticsStatus.CONNECTED, List.copyOf(entries));
        } catch (Exception e) {
            log.warn("[Sentry] 릴리즈 파싱 실패: project={}, {}", project, e.getClass().getSimpleName());
            return new SentryIssueReport.ResourceCatalog(AnalyticsStatus.UNAVAILABLE, List.of());
        }
    }

    private URI releaseUri(String project) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/0/organizations/{organization}/releases/")
                .queryParam("project", project)
                .queryParam("per_page", 20)
                .buildAndExpand(organization)
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

    private List<SentryIssueReport.Day> parseDatasetDays(
            String body, LocalDate from, LocalDate to) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode timeSeries = root.path("timeSeries");
        if (!timeSeries.isArray()) {
            throw new IllegalArgumentException("Sentry timeseries response must contain timeSeries.");
        }
        Map<LocalDate, Long> counts = new java.util.HashMap<>();
        for (JsonNode series : timeSeries) {
            JsonNode values = series.path("values");
            if (!values.isArray()) {
                continue;
            }
            for (JsonNode point : values) {
                if (!point.path("timestamp").isNumber() || !point.path("value").isNumber()) {
                    throw new IllegalArgumentException("Sentry timeseries point must contain timestamp and value.");
                }
                LocalDate date = Instant.ofEpochMilli(point.path("timestamp").asLong())
                        .atZone(ZoneOffset.UTC).toLocalDate();
                if (!date.isBefore(from) && !date.isAfter(to)) {
                    counts.merge(date, point.path("value").asLong(), Long::sum);
                }
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

    private List<SentryIssueReport.Event> parseEvents(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (!root.isArray()) {
            throw new IllegalArgumentException("Sentry event response must be an array.");
        }

        List<SentryIssueReport.Event> parsed = new ArrayList<>();
        for (JsonNode node : root) {
            List<SentryIssueReport.Tag> tags = new ArrayList<>();
            JsonNode tagNodes = node.path("tags");
            if (tagNodes.isArray()) {
                for (JsonNode tag : tagNodes) {
                    tags.add(new SentryIssueReport.Tag(text(tag, "key"), text(tag, "value")));
                }
            }
            Map<String, Object> metadata = node.path("metadata").isObject()
                    ? objectMapper.convertValue(node.path("metadata"), new TypeReference<>() { })
                    : Map.of();
            Map<String, Object> details = objectMapper.convertValue(node, new TypeReference<>() { });
            parsed.add(new SentryIssueReport.Event(
                    text(node, "eventID"),
                    text(node, "id"),
                    text(node, "groupID"),
                    text(node, "projectID"),
                    text(node, "title"),
                    text(node, "message"),
                    text(node, "platform"),
                    text(node, "event.type"),
                    text(node, "location"),
                    text(node, "culprit"),
                    text(node, "crashFile"),
                    instant(node, "dateCreated"),
                    List.copyOf(tags),
                    metadata,
                    details));
            if (parsed.size() == RECENT_EVENT_LIMIT) {
                break;
            }
        }
        return parsed;
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
