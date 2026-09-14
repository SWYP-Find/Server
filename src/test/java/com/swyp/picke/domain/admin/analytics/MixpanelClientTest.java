package com.swyp.picke.domain.admin.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
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

    /** 원본 export 는 NDJSON 이며 content-type 이 JSON 이 아닐 수 있다. 본문만 보고 판단한다. */
    private AnalyticsHttpResponse response(int status, String body) {
        return new AnalyticsHttpResponse(status, Map.of("content-type", List.of("text/plain")), body);
    }

    private MixpanelClient client(String secret, List<String> defaultEvents, AnalyticsHttpTransport transport) {
        return new MixpanelClient("https://data.mixpanel.com", secret, ZoneId.of("UTC"),
                defaultEvents, transport, new ObjectMapper());
    }

    /** properties.time 은 프로젝트 타임존 기준 epoch 초다. 아래 값은 UTC 자정 직후를 가리킨다. */
    private String line(String event, String date, int hour) {
        long epoch = LocalDate.parse(date).atStartOfDay(ZoneId.of("UTC")).plusHours(hour).toEpochSecond();
        return "{\"event\":\"" + event + "\",\"properties\":{\"time\":" + epoch
                + ",\"distinct_id\":\"user-" + hour + "\"}}";
    }

    @Test
    @DisplayName("API 비밀이 없으면 호출하지 않고 NOT_CONFIGURED 를 준다")
    void returnsNotConfiguredWithoutSecret() {
        var transport = new CapturingTransport(response(200, ""));

        var result = client("", List.of(), transport).fetchDailyCounts(null, from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.NOT_CONFIGURED);
        assertThat(transport.uri).isNull();
    }

    @Test
    @DisplayName("원본 이벤트를 일별로 센다. 이벤트가 없던 날짜는 미집계가 아니라 0 이다")
    void countsRawEventsPerDay() {
        var transport = new CapturingTransport(response(200, String.join("\n",
                line("screen_view", "2026-09-08", 1),
                line("screen_view", "2026-09-08", 5),
                line("screen_view", "2026-09-10", 3),
                line("sign_up", "2026-09-10", 4))));

        var result = client("secret", List.of(), transport)
                .fetchDailyCounts(List.of("screen_view"), from, to);

        String expected = Base64.getEncoder().encodeToString("secret:".getBytes(StandardCharsets.UTF_8));
        assertThat(transport.headers).containsEntry("Authorization", "Basic " + expected);
        assertThat(transport.uri.toString())
                .contains("data.mixpanel.com/api/2.0/export")
                .contains("from_date=2026-09-08")
                .contains("to_date=2026-09-10")
                // 레거시 비밀 인증에 project_id 를 넣으면 400 이 된다.
                .doesNotContain("project_id");
        assertThat(result.status()).isEqualTo(AnalyticsStatus.CONNECTED);
        assertThat(result.events()).hasSize(1);
        assertThat(result.availableEvents()).containsExactly("screen_view", "sign_up");
        assertThat(result.totalEvents()).isEqualTo(3);
        assertThat(result.uniqueUsers()).isEqualTo(3);
        assertThat(result.summaryDate()).isEqualTo(to);
        assertThat(result.activeUsers()).isEqualTo(2);
        assertThat(result.signUps()).isEqualTo(1);
        assertThat(result.signUpDays())
                .extracting(MixpanelEventReport.SignUpDay::date, MixpanelEventReport.SignUpDay::count)
                .containsExactly(tuple(LocalDate.of(2026, 9, 8), 0L),
                                 tuple(LocalDate.of(2026, 9, 9), 0L),
                                 tuple(LocalDate.of(2026, 9, 10), 1L));
        assertThat(result.events().getFirst().total()).isEqualTo(3);
        assertThat(result.events().getFirst().uniqueUsers()).isEqualTo(3);
        assertThat(result.events().getFirst().firstSeen()).isNotNull();
        assertThat(result.events().getFirst().lastSeen()).isNotNull();
        assertThat(result.events().getFirst().days())
                .extracting(MixpanelEventReport.Day::date, MixpanelEventReport.Day::count)
                .containsExactly(tuple(LocalDate.of(2026, 9, 10), 1L),
                                 tuple(LocalDate.of(2026, 9, 9), 0L),
                                 tuple(LocalDate.of(2026, 9, 8), 2L));
        assertThat(result.events().getFirst().days())
                .extracting(MixpanelEventReport.Day::uniqueUsers)
                .containsExactly(1L, 0L, 2L);
    }

    @Test
    @DisplayName("이벤트를 지정하지 않으면 기간에 나타난 이벤트 전부를 발생 수 내림차순으로 센다")
    void countsEveryEventWhenNoneRequested() {
        var transport = new CapturingTransport(response(200, String.join("\n",
                line("screen_view", "2026-09-08", 1),
                line("screen_view", "2026-09-09", 2),
                line("ui_action", "2026-09-09", 2),
                line("sign_up", "2026-09-10", 2),
                line("sign_up", "2026-09-10", 3),
                line("sign_up", "2026-09-10", 4))));

        var result = client("secret", List.of(), transport).fetchDailyCounts(null, from, to);

        assertThat(result.events())
                .extracting(MixpanelEventReport.EventSeries::event, MixpanelEventReport.EventSeries::total)
                .containsExactly(tuple("sign_up", 3L), tuple("screen_view", 2L), tuple("ui_action", 1L));
    }

    @Test
    @DisplayName("요청 이벤트가 기간에 없으면 0 으로 채운 시리즈를 준다. 이벤트를 통째로 빼지 않는다")
    void keepsRequestedEventWithoutData() {
        var transport = new CapturingTransport(response(200, line("screen_view", "2026-09-08", 1)));

        var result = client("secret", List.of(), transport)
                .fetchDailyCounts(List.of("battle_step"), from, to);

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().getFirst().event()).isEqualTo("battle_step");
        assertThat(result.events().getFirst().total()).isZero();
        assertThat(result.events().getFirst().days()).hasSize(3)
                .extracting(MixpanelEventReport.Day::count).containsOnly(0L);
    }

    @Test
    @DisplayName("타임존 차이로 경계를 넘어온 이벤트는 버린다")
    void dropsEventsOutsideRequestedRange() {
        var transport = new CapturingTransport(response(200, String.join("\n",
                line("screen_view", "2026-09-10", 23),
                line("screen_view", "2026-09-11", 8))));

        var result = client("secret", List.of(), transport)
                .fetchDailyCounts(List.of("screen_view"), from, to);

        assertThat(result.events().getFirst().total()).isEqualTo(1);
    }

    @Test
    @DisplayName("플랜 제한이나 자격 거절이면 UNAVAILABLE 이다. 0 으로 보여주지 않는다")
    void reportsUnavailableOnRejectedRequest() {
        var transport = new CapturingTransport(response(402,
                "{\"error\":\"Your plan does not allow API calls.\"}"));

        var result = client("secret", List.of(), transport).fetchDailyCounts(null, from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.UNAVAILABLE);
        assertThat(result.events()).isEmpty();
    }

    @Test
    @DisplayName("본문 모양이 다르면 UNAVAILABLE 이다")
    void reportsUnavailableOnUnexpectedBody() {
        var transport = new CapturingTransport(response(200, "{\"status\":\"ok\"}"));

        var result = client("secret", List.of(), transport).fetchDailyCounts(null, from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.UNAVAILABLE);
    }
}
