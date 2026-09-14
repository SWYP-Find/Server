package com.swyp.picke.domain.admin.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SentryClientTest {

    private final LocalDate from = LocalDate.of(2026, 9, 8);
    private final LocalDate to = LocalDate.of(2026, 9, 10);

    /** 프로젝트 슬러그별로 다른 응답을 준다. 슬러그에 맞는 응답이 없으면 마지막 등록 응답을 쓴다. */
    private static class CapturingTransport implements AnalyticsHttpTransport {
        private final Map<String, AnalyticsHttpResponse> byProject = new java.util.LinkedHashMap<>();
        private AnalyticsHttpResponse statsResponse = new AnalyticsHttpResponse(
                200,
                Map.of("content-type", List.of("application/json")),
                "[[1788825600,25],[1788912000,40],[1788998400,60]]");
        private AnalyticsHttpResponse eventResponse = new AnalyticsHttpResponse(
                200, Map.of("content-type", List.of("application/json")), "[]");
        private AnalyticsHttpResponse analyticsEventCatalogResponse = new AnalyticsHttpResponse(
                200, Map.of("content-type", List.of("application/json")), "{\"data\":[]}");
        private AnalyticsHttpResponse analyticsEventTimeseriesResponse = new AnalyticsHttpResponse(
                200, Map.of("content-type", List.of("application/json")), "{\"timeSeries\":[]}");
        private AnalyticsHttpResponse datasetResponse = new AnalyticsHttpResponse(
                200, Map.of("content-type", List.of("application/json")), """
                {"timeSeries":[{"values":[
                  {"timestamp":1788825600000,"value":1},
                  {"timestamp":1788912000000,"value":2},
                  {"timestamp":1788998400000,"value":3}
                ]}]}
                """);
        private AnalyticsHttpResponse metricResponse = new AnalyticsHttpResponse(
                200, Map.of("content-type", List.of("application/json")),
                "[{\"name\":\"app.launch.count\",\"type\":\"counter\",\"unit\":null,\"count\":8}]");
        private AnalyticsHttpResponse sessionResponse = new AnalyticsHttpResponse(
                200, Map.of("content-type", List.of("application/json")), """
                {"intervals":["2026-09-08T00:00:00Z","2026-09-09T00:00:00Z","2026-09-10T00:00:00Z"],
                 "groups":[{"by":{"session.status":"healthy"},"totals":{"sum(session)":9},
                 "series":{"sum(session)":[2,3,4]}}]}
                """);
        private AnalyticsHttpResponse releaseResponse = new AnalyticsHttpResponse(
                200, Map.of("content-type", List.of("application/json")),
                "[{\"version\":\"picke-ios@1.2.3+45\",\"status\":\"open\"}]");
        final List<URI> uris = new java.util.ArrayList<>();
        Map<String, String> headers;

        CapturingTransport(AnalyticsHttpResponse response) {
            byProject.put("*", response);
        }

        CapturingTransport with(String project, AnalyticsHttpResponse response) {
            byProject.put(project, response);
            return this;
        }

        CapturingTransport withStats(AnalyticsHttpResponse response) {
            statsResponse = response;
            return this;
        }

        CapturingTransport withEvents(AnalyticsHttpResponse response) {
            eventResponse = response;
            return this;
        }

        CapturingTransport withAnalyticsEvents(
                AnalyticsHttpResponse catalogResponse,
                AnalyticsHttpResponse timeseriesResponse) {
            analyticsEventCatalogResponse = catalogResponse;
            analyticsEventTimeseriesResponse = timeseriesResponse;
            return this;
        }

        @Override
        public AnalyticsHttpResponse get(URI uri, Map<String, String> headers) {
            this.uris.add(uri);
            this.headers = headers;
            if (uri.getPath().endsWith("/stats/")) {
                return statsResponse;
            }
            if (uri.getPath().contains("/organizations/") && uri.getPath().endsWith("/events/")) {
                return analyticsEventCatalogResponse;
            }
            if (uri.getPath().endsWith("/events/")) {
                return eventResponse;
            }
            if (uri.getPath().endsWith("/events-stats/")) {
                return analyticsEventTimeseriesResponse;
            }
            if (uri.getPath().endsWith("/events-timeseries/")) {
                return datasetResponse;
            }
            if (uri.getPath().endsWith("/trace-items/metrics/")) {
                return metricResponse;
            }
            if (uri.getPath().endsWith("/sessions/")) {
                return sessionResponse;
            }
            if (uri.getPath().endsWith("/releases/")) {
                return releaseResponse;
            }
            return byProject.entrySet().stream()
                    .filter(entry -> uri.getPath().contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(byProject.get("*"));
        }
    }

    private AnalyticsHttpResponse response(int status, String contentType, String body) {
        return new AnalyticsHttpResponse(status, Map.of("content-type", List.of(contentType)), body);
    }

    private SentryClient client(String token, AnalyticsHttpTransport transport) {
        return new SentryClient("https://sentry.io", "picke", List.of("picke-ios", "picke-android"),
                token, transport, new ObjectMapper());
    }

    @Test
    @DisplayName("토큰이 없으면 호출하지 않고 NOT_CONFIGURED 를 준다")
    void returnsNotConfiguredWithoutToken() {
        var transport = new CapturingTransport(response(200, "application/json", "[]"));

        var result = client("", transport).fetchUnresolvedIssues(from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.NOT_CONFIGURED);
        assertThat(result.projects()).isEmpty();
        assertThat(transport.uris).isEmpty();
    }

    @Test
    @DisplayName("iOS·Android 를 프로젝트별로 나눠 읽는다. 절대 기간을 쓰려면 statsPeriod 를 비워 보낸다")
    void fetchesEveryProjectWithAbsoluteRange() {
        var transport = new CapturingTransport(response(200, "application/json", """
                [
                  {"id":"1","title":"Crash A","culprit":"VoteView","level":"error","count":"120",
                   "userCount":31,"firstSeen":"2026-09-08T01:00:00Z","lastSeen":"2026-09-10T04:00:00Z",
                   "permalink":"https://sentry.io/organizations/picke/issues/1/"},
                  {"id":"2","title":"Crash B","culprit":null,"level":"fatal","count":5,
                   "userCount":null,"firstSeen":"2026-09-09T01:00:00Z","lastSeen":"2026-09-09T02:00:00Z",
                   "permalink":null}
                ]
                """)).withEvents(response(200, "application/json", """
                [
                  {"eventID":"event-a","id":"event-a","groupID":"1","projectID":"11",
                   "title":"Crash A","message":"Fatal signal","platform":"cocoa","event.type":"error",
                   "location":"VoteView.swift","culprit":"VoteView","crashFile":"app.dSYM",
                   "dateCreated":"2026-09-10T04:00:00Z",
                   "tags":[{"key":"environment","value":"production"},{"key":"release","value":"1.2.3"}],
                   "metadata":{"filename":"VoteView.swift","function":"submitVote"},
                   "user":{"email":"should-not-be-copied@example.com"}}
                ]
                """));

        var result = client("token", transport).fetchUnresolvedIssues(from, to);

        assertThat(transport.headers).containsEntry("Authorization", "Bearer token");
        assertThat(transport.uris).hasSize(26);
        assertThat(transport.uris.getFirst().toString())
                .contains("/api/0/projects/picke/picke-ios/issues/")
                .contains("query=is:unresolved")
                .contains("sort=freq")
                .contains("statsPeriod=")
                .contains("start=2026-09-08T00:00")
                .contains("end=2026-09-10T23:59:59");
        assertThat(transport.uris).anyMatch(uri -> uri.toString()
                .contains("/api/0/projects/picke/picke-android/issues/"));
        assertThat(transport.uris).anyMatch(uri -> uri.toString()
                .contains("/api/0/projects/picke/picke-ios/stats/")
                && uri.toString().contains("resolution=1d")
                && uri.toString().contains("stat=received"));
        assertThat(transport.uris).anyMatch(uri -> uri.toString()
                .contains("/api/0/projects/picke/picke-ios/events/")
                && uri.toString().contains("start=2026-09-08T00:00")
                && uri.toString().contains("end=2026-09-10T23:59:59")
                && uri.toString().contains("full=true"));
        assertThat(transport.uris).anyMatch(uri -> uri.toString()
                .contains("/api/0/organizations/picke/events-timeseries/")
                && uri.toString().contains("project=picke-ios")
                && uri.toString().contains("dataset=logs")
                && uri.toString().contains("yAxis=count()"));
        assertThat(transport.uris).anyMatch(uri -> uri.toString()
                .contains("/api/0/organizations/picke/events-timeseries/")
                && uri.toString().contains("project=picke-ios")
                && uri.toString().contains("dataset=tracemetrics")
                && uri.toString().contains("yAxis=sum(value)"));
        assertThat(transport.uris).anyMatch(uri -> uri.toString()
                .contains("/api/0/organizations/picke/events-timeseries/")
                && uri.toString().contains("project=picke-ios")
                && uri.toString().contains("dataset=tracemetrics")
                && uri.toString().contains("yAxis=sum(value)")
                && uri.toString().contains("event:sign_up"));
        assertThat(result.status()).isEqualTo(AnalyticsStatus.CONNECTED);
        assertThat(result.fetchedAt()).isNotNull();
        assertThat(result.projects()).extracting(SentryIssueReport.ProjectIssues::project,
                                                 SentryIssueReport.ProjectIssues::totalEvents)
                .containsExactly(tuple("picke-ios", 125L), tuple("picke-android", 125L));
        // 두 프로젝트 합계다. 프로젝트별 합계와 구분한다.
        assertThat(result.totalEvents()).isEqualTo(250);
        assertThat(result.projects().getFirst().days())
                .extracting(SentryIssueReport.Day::date, SentryIssueReport.Day::events)
                .containsExactly(tuple(LocalDate.of(2026, 9, 8), 25L),
                                 tuple(LocalDate.of(2026, 9, 9), 40L),
                                 tuple(LocalDate.of(2026, 9, 10), 60L));
        assertThat(result.projects().getFirst().unresolvedEvents()).isEqualTo(125);
        var issues = result.projects().getFirst().issues();
        assertThat(issues).extracting(SentryIssueReport.Issue::title, SentryIssueReport.Issue::events)
                .containsExactly(tuple("Crash A", 120L), tuple("Crash B", 5L));
        assertThat(issues.getFirst().users()).isEqualTo(31);
        assertThat(issues.getLast().users()).isNull();
        var event = result.projects().getFirst().recentEvents().getFirst();
        assertThat(event.eventId()).isEqualTo("event-a");
        assertThat(event.groupId()).isEqualTo("1");
        assertThat(event.platform()).isEqualTo("cocoa");
        assertThat(event.type()).isEqualTo("error");
        assertThat(event.tags()).extracting(SentryIssueReport.Tag::key, SentryIssueReport.Tag::value)
                .containsExactly(tuple("environment", "production"), tuple("release", "1.2.3"));
        assertThat(event.metadata()).containsEntry("filename", "VoteView.swift")
                .containsEntry("function", "submitVote");
        assertThat(event.details()).containsKey("user").containsKey("tags").containsKey("metadata");
        assertThat(result.projects().getFirst().datasets())
                .extracting(SentryIssueReport.DatasetSeries::dataset,
                            SentryIssueReport.DatasetSeries::status,
                            SentryIssueReport.DatasetSeries::total)
                .containsExactly(
                        tuple("errors", AnalyticsStatus.CONNECTED, 6L),
                        tuple("logs", AnalyticsStatus.CONNECTED, 6L),
                        tuple("spans", AnalyticsStatus.CONNECTED, 6L),
                        tuple("profile_functions", AnalyticsStatus.CONNECTED, 6L),
                        tuple("tracemetrics", AnalyticsStatus.CONNECTED, 6L),
                        tuple("sign_up", AnalyticsStatus.CONNECTED, 6L));
        assertThat(result.projects().getFirst().analyticsEvents().status())
                .isEqualTo(AnalyticsStatus.CONNECTED);
        assertThat(result.projects().getFirst().analyticsEvents().events())
                .extracting(SentryIssueReport.AnalyticsEventSeries::event,
                            SentryIssueReport.AnalyticsEventSeries::total,
                            SentryIssueReport.AnalyticsEventSeries::uniqueUsers)
                .containsExactly(tuple("sign_up", 6L, null));
        assertThat(result.projects().getFirst().analyticsEvents().events().getFirst().days())
                .extracting(SentryIssueReport.AnalyticsEventDay::uniqueUsers)
                .containsOnlyNulls();
        assertThat(result.projects().getFirst().metricCatalog().entries().getFirst())
                .containsEntry("name", "app.launch.count")
                .containsEntry("count", 8);
        assertThat(result.projects().getFirst().sessionHealth().series().getFirst().status())
                .isEqualTo("healthy");
        assertThat(result.projects().getFirst().sessionHealth().series().getFirst().total()).isEqualTo(9);
        assertThat(result.projects().getFirst().releases().entries().getFirst())
                .containsEntry("version", "picke-ios@1.2.3+45");
    }


    @Test
    @DisplayName("analytics_event 태그로 수집한 모든 액션을 발생 수와 사용자 일별 추이로 묶는다")
    void groupsEveryAnalyticsEventLikeMixpanel() {
        var transport = new CapturingTransport(response(200, "application/json", "[]"))
                .withAnalyticsEvents(
                        response(200, "application/json", """
                                {"data":[
                                  {"analytics_event":"ui_action","count()":7,
                                   "count_unique(user)":3,"min(timestamp)":"2026-09-08T01:00:00Z",
                                   "max(timestamp)":"2026-09-10T02:00:00Z"},
                                  {"analytics_event":"sign_up","count()":"2",
                                   "count_unique(user)":2,"min(timestamp)":"2026-09-09T03:00:00Z",
                                   "max(timestamp)":"2026-09-10T04:00:00Z"}
                                ]}
                                """),
                        response(200, "application/json", """
                                {
                                  "ui_action":{
                                    "count()":{"data":[[1788825600,[{"count":1}]],[1788912000,[{"count":2}]],[1788998400,[{"count":4}]]]},
                                    "count_unique(user)":{"data":[[1788825600,[{"count":1}]],[1788912000,[{"count":1}]],[1788998400,[{"count":2}]]]}
                                  },
                                  "sign_up":{
                                    "count()":{"data":[[1788912000,[{"count":1}]],[1788998400,[{"count":1}]]]},
                                    "count_unique(user)":{"data":[[1788912000,[{"count":1}]],[1788998400,[{"count":1}]]]}
                                  }
                                }
                                """));

        var result = client("token", transport).fetchUnresolvedIssues(from, to);

        assertThat(transport.uris).anyMatch(uri -> uri.getPath().endsWith("/events/")
                && uri.getPath().contains("/organizations/")
                && uri.getQuery().contains("field=analytics_event")
                && uri.getQuery().contains("query=has:analytics_event"));
        assertThat(transport.uris).anyMatch(uri -> uri.getPath().endsWith("/events-stats/")
                && uri.getQuery().contains("field=analytics_event")
                && uri.getQuery().contains("topEvents=2")
                && uri.getQuery().contains("sort=analytics_event")
                && uri.getQuery().contains("partial=1")
                && uri.getQuery().contains("yAxis=count_unique(user)"));
        var analytics = result.projects().getFirst().analyticsEvents();
        assertThat(analytics.status()).isEqualTo(AnalyticsStatus.CONNECTED);
        assertThat(analytics.totalEvents()).isEqualTo(9);
        assertThat(analytics.events())
                .extracting(SentryIssueReport.AnalyticsEventSeries::event,
                            SentryIssueReport.AnalyticsEventSeries::total,
                            SentryIssueReport.AnalyticsEventSeries::uniqueUsers)
                .containsExactly(tuple("ui_action", 7L, 3L), tuple("sign_up", 2L, 2L));
        assertThat(analytics.events().getLast().days())
                .extracting(SentryIssueReport.AnalyticsEventDay::date,
                            SentryIssueReport.AnalyticsEventDay::count,
                            SentryIssueReport.AnalyticsEventDay::uniqueUsers)
                .containsExactly(
                        tuple(LocalDate.of(2026, 9, 8), 0L, 0L),
                        tuple(LocalDate.of(2026, 9, 9), 1L, 1L),
                        tuple(LocalDate.of(2026, 9, 10), 1L, 1L));
    }

    @Test
    @DisplayName("응답 순서를 믿지 않고 이벤트 수 내림차순으로 다시 정렬한다. sort=freq 가 절대 기간에서 순서를 보장하지 않는다")
    void sortsIssuesByEventsDescending() {
        var transport = new CapturingTransport(response(200, "application/json", """
                [
                  {"id":"1","title":"적게","count":"38"},
                  {"id":"2","title":"많이","count":"101"},
                  {"id":"3","title":"모름","count":null},
                  {"id":"4","title":"중간","count":"50"}
                ]
                """));

        var result = client("token", transport).fetchUnresolvedIssues(from, to);

        assertThat(result.projects().getFirst().issues()).extracting(SentryIssueReport.Issue::title)
                .containsExactly("많이", "중간", "적게", "모름");
    }

    @Test
    @DisplayName("한 프로젝트가 막혀도 다른 프로젝트는 살린다. 전체 합계는 비운다")
    void keepsHealthyProjectWhenOtherFails() {
        var transport = new CapturingTransport(response(200, "application/json",
                "[{\"id\":\"1\",\"title\":\"Android Crash\",\"count\":\"7\"}]"))
                .with("picke-ios", response(403, "application/json", "{\"detail\":\"forbidden\"}"));

        var result = client("token", transport).fetchUnresolvedIssues(from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.UNAVAILABLE);
        assertThat(result.totalEvents()).isNull();
        assertThat(result.projects()).extracting(SentryIssueReport.ProjectIssues::project,
                                                 SentryIssueReport.ProjectIssues::status)
                .containsExactly(tuple("picke-ios", AnalyticsStatus.UNAVAILABLE),
                                 tuple("picke-android", AnalyticsStatus.CONNECTED));
        assertThat(result.projects().getLast().issues()).hasSize(1);
    }

    @Test
    @DisplayName("토큰이 거절되면 UNAVAILABLE 이다. 0건으로 보여주지 않는다")
    void reportsUnavailableOnRejectedToken() {
        var transport = new CapturingTransport(response(401, "application/json", "{\"detail\":\"invalid\"}"));

        var result = client("expired", transport).fetchUnresolvedIssues(from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.UNAVAILABLE);
        assertThat(result.totalEvents()).isNull();
        assertThat(result.projects()).allMatch(project -> project.issues().isEmpty());
    }

    @Test
    @DisplayName("응답 모양이 다르면 UNAVAILABLE 이다")
    void reportsUnavailableOnUnexpectedSchema() {
        var transport = new CapturingTransport(response(200, "application/json", "{\"issues\":[]}"));

        var result = client("token", transport).fetchUnresolvedIssues(from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.UNAVAILABLE);
        assertThat(result.projects()).extracting(SentryIssueReport.ProjectIssues::status)
                .containsOnly(AnalyticsStatus.UNAVAILABLE);
    }

    @Test
    @DisplayName("일별 통계 응답이 잘못되면 0건으로 꾸미지 않고 UNAVAILABLE 이다")
    void reportsUnavailableOnUnexpectedStatsSchema() {
        var transport = new CapturingTransport(response(200, "application/json", "[]"))
                .withStats(response(200, "application/json", "{\"points\":[]}"));

        var result = client("token", transport).fetchUnresolvedIssues(from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.UNAVAILABLE);
        assertThat(result.projects()).allMatch(project -> project.days().isEmpty());
    }
}
