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
        final List<URI> uris = new java.util.ArrayList<>();
        Map<String, String> headers;

        CapturingTransport(AnalyticsHttpResponse response) {
            byProject.put("*", response);
        }

        CapturingTransport with(String project, AnalyticsHttpResponse response) {
            byProject.put(project, response);
            return this;
        }

        @Override
        public AnalyticsHttpResponse get(URI uri, Map<String, String> headers) {
            this.uris.add(uri);
            this.headers = headers;
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
                """));

        var result = client("token", transport).fetchUnresolvedIssues(from, to);

        assertThat(transport.headers).containsEntry("Authorization", "Bearer token");
        assertThat(transport.uris).hasSize(2);
        assertThat(transport.uris.getFirst().toString())
                .contains("/api/0/projects/picke/picke-ios/issues/")
                .contains("query=is:unresolved")
                .contains("sort=freq")
                .contains("statsPeriod=")
                .contains("start=2026-09-08T00:00")
                .contains("end=2026-09-10T23:59:59");
        assertThat(transport.uris.getLast().toString())
                .contains("/api/0/projects/picke/picke-android/issues/");
        assertThat(result.status()).isEqualTo(AnalyticsStatus.CONNECTED);
        assertThat(result.fetchedAt()).isNotNull();
        assertThat(result.projects()).extracting(SentryIssueReport.ProjectIssues::project,
                                                 SentryIssueReport.ProjectIssues::totalEvents)
                .containsExactly(tuple("picke-ios", 125L), tuple("picke-android", 125L));
        // 두 프로젝트 합계다. 프로젝트별 합계와 구분한다.
        assertThat(result.totalEvents()).isEqualTo(250);
        var issues = result.projects().getFirst().issues();
        assertThat(issues).extracting(SentryIssueReport.Issue::title, SentryIssueReport.Issue::events)
                .containsExactly(tuple("Crash A", 120L), tuple("Crash B", 5L));
        assertThat(issues.getFirst().users()).isEqualTo(31);
        assertThat(issues.getLast().users()).isNull();
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
}
