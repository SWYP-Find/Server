package com.swyp.picke.domain.admin.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class MixpanelClientTest {

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

    private AnalyticsHttpResponse response(int status, String body) {
        return new AnalyticsHttpResponse(status, Map.of("content-type", List.of("application/json")), body);
    }

    private MixpanelClient client(String secret, List<String> defaultEvents, AnalyticsHttpTransport transport) {
        return new MixpanelClient("https://mixpanel.com", "3123456", "picke-sa", secret,
                defaultEvents, transport, new ObjectMapper());
    }

    @Test
    @DisplayName("서비스 계정이 없으면 호출하지 않고 NOT_CONFIGURED 를 준다")
    void returnsNotConfiguredWithoutCredentials() {
        var transport = new CapturingTransport(response(200, "{}"));

        var result = client("", List.of("앱 실행"), transport).fetchDailyCounts(null, from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.NOT_CONFIGURED);
        assertThat(transport.uri).isNull();
    }

    @Test
    @DisplayName("조회할 이벤트 이름이 없으면 NOT_CONFIGURED 다. 서버는 앱이 무엇을 트래킹하는지 모른다")
    void returnsNotConfiguredWithoutEventNames() {
        var transport = new CapturingTransport(response(200, "{}"));

        var result = client("secret", List.of(), transport).fetchDailyCounts(null, from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.NOT_CONFIGURED);
        assertThat(transport.uri).isNull();
    }

    @Test
    @DisplayName("일별 발생 수를 최신 날짜부터 읽는다. 값이 없는 날짜는 null 이며 0 으로 대체하지 않는다")
    void fetchesDailyCountsNewestFirst() {
        var transport = new CapturingTransport(response(200, """
                {"data":{"series":["2026-09-08","2026-09-09","2026-09-10"],
                 "values":{"앱 실행":{"2026-09-08":10,"2026-09-10":30}}},"legend_size":1}
                """));

        var result = client("secret", List.of(), transport).fetchDailyCounts(List.of("앱 실행"), from, to);

        String expectedCredentials = Base64.getEncoder()
                .encodeToString("picke-sa:secret".getBytes(StandardCharsets.UTF_8));
        assertThat(transport.headers).containsEntry("Authorization", "Basic " + expectedCredentials);
        assertThat(transport.uri.toString())
                .contains("/api/query/segmentation")
                .contains("project_id=3123456")
                .contains("from_date=2026-09-08")
                .contains("to_date=2026-09-10")
                .contains("unit=day");
        assertThat(result.status()).isEqualTo(AnalyticsStatus.CONNECTED);
        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().total()).isEqualTo(40);
        assertThat(result.events().getFirst().days())
                .extracting(MixpanelEventReport.Day::date, MixpanelEventReport.Day::count)
                .containsExactly(tuple(LocalDate.of(2026, 9, 10), 30L),
                                 tuple(LocalDate.of(2026, 9, 9), null),
                                 tuple(LocalDate.of(2026, 9, 8), 10L));
    }

    @Test
    @DisplayName("요청에 이벤트가 오면 기본 설정 이벤트 대신 그걸 쓴다")
    void requestedEventsOverrideDefaults() {
        var transport = new CapturingTransport(response(200,
                "{\"data\":{\"series\":[],\"values\":{\"투표 완료\":{}}}}"));

        client("secret", List.of("앱 실행"), transport).fetchDailyCounts(List.of("투표 완료"), from, to);

        assertThat(transport.uri.toString()).contains("event=%ED%88%AC%ED%91%9C%20%EC%99%84%EB%A3%8C");
    }

    @Test
    @DisplayName("자격이 거절되면 UNAVAILABLE 이다")
    void reportsUnavailableOnRejectedCredentials() {
        var transport = new CapturingTransport(response(401, "{\"error\":\"invalid\"}"));

        var result = client("wrong", List.of("앱 실행"), transport).fetchDailyCounts(null, from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.UNAVAILABLE);
        assertThat(result.events()).isEmpty();
    }

    @Test
    @DisplayName("이벤트 하나라도 못 읽으면 전체를 UNAVAILABLE 로 둔다. 일부 합계를 전체처럼 보여주지 않는다")
    void reportsUnavailableWhenAnyEventMissing() {
        var transport = new CapturingTransport(response(200, "{\"data\":{\"values\":{}}}"));

        var result = client("secret", List.of(), transport)
                .fetchDailyCounts(List.of("앱 실행", "투표 완료"), from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.UNAVAILABLE);
    }
}
