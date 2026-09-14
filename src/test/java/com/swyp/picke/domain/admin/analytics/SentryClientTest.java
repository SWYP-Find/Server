package com.swyp.picke.domain.admin.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SentryClientTest {

    private final LocalDate from = LocalDate.of(2026, 9, 8);
    private final LocalDate to = LocalDate.of(2026, 9, 10);

    private static class CapturingTransport implements AnalyticsHttpTransport {
        private final AnalyticsHttpResponse response;
        URI uri;
        Map<String, String> headers;

        CapturingTransport(AnalyticsHttpResponse response) {
            this.response = response;
        }

        @Override
        public AnalyticsHttpResponse get(URI uri, Map<String, String> headers) {
            this.uri = uri;
            this.headers = headers;
            return response;
        }
    }

    private AnalyticsHttpResponse response(int status, String contentType, String body) {
        return new AnalyticsHttpResponse(status, Map.of("content-type", List.of(contentType)), body);
    }

    private SentryClient client(String token, AnalyticsHttpTransport transport) {
        return new SentryClient("https://sentry.io", "picke", "picke-ios", token, transport, new ObjectMapper());
    }

    @Test
    @DisplayName("토큰이 없으면 호출하지 않고 NOT_CONFIGURED 를 준다")
    void returnsNotConfiguredWithoutToken() {
        var transport = new CapturingTransport(response(200, "application/json", "[]"));

        var result = client("", transport).fetchUnresolvedIssues(from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.NOT_CONFIGURED);
        assertThat(result.issues()).isEmpty();
        assertThat(transport.uri).isNull();
    }

    @Test
    @DisplayName("미해결 이슈를 이벤트 수 내림차순으로 읽는다. 절대 기간을 쓰려면 statsPeriod 를 비워 보낸다")
    void fetchesUnresolvedIssuesWithAbsoluteRange() {
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
        assertThat(transport.uri.toString())
                .contains("/api/0/projects/picke/picke-ios/issues/")
                .contains("query=is:unresolved")
                .contains("sort=freq")
                .contains("statsPeriod=")
                .contains("start=2026-09-08T00:00")
                .contains("end=2026-09-10T23:59:59");
        assertThat(result.status()).isEqualTo(AnalyticsStatus.CONNECTED);
        assertThat(result.fetchedAt()).isNotNull();
        assertThat(result.totalEvents()).isEqualTo(125);
        assertThat(result.issues()).extracting(SentryIssueReport.Issue::title, SentryIssueReport.Issue::events)
                .containsExactly(org.assertj.core.api.Assertions.tuple("Crash A", 120L),
                                 org.assertj.core.api.Assertions.tuple("Crash B", 5L));
        assertThat(result.issues().getFirst().users()).isEqualTo(31);
        assertThat(result.issues().getLast().users()).isNull();
    }

    @Test
    @DisplayName("토큰이 거절되면 UNAVAILABLE 이다. 0건으로 보여주지 않는다")
    void reportsUnavailableOnRejectedToken() {
        var transport = new CapturingTransport(response(401, "application/json", "{\"detail\":\"invalid\"}"));

        var result = client("expired", transport).fetchUnresolvedIssues(from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.UNAVAILABLE);
        assertThat(result.totalEvents()).isNull();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    @DisplayName("응답 모양이 다르면 UNAVAILABLE 이다")
    void reportsUnavailableOnUnexpectedSchema() {
        var transport = new CapturingTransport(response(200, "application/json", "{\"issues\":[]}"));

        var result = client("token", transport).fetchUnresolvedIssues(from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.UNAVAILABLE);
    }
}
