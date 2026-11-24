package com.swyp.picke.domain.admin.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class MixpanelFunnelTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final List<String> STEPS =
            List.of("battle_step", "screen_view", "community_action");

    private final LocalDate from = LocalDate.of(2026, 8, 1);
    private final LocalDate to = LocalDate.of(2026, 8, 31);

    private static class StubTransport implements AnalyticsHttpTransport {
        private final AnalyticsHttpResponse response;
        URI uri;

        StubTransport(AnalyticsHttpResponse response) {
            this.response = response;
        }

        @Override
        public AnalyticsHttpResponse get(URI uri, Map<String, String> headers) {
            this.uri = uri;
            return response;
        }
    }

    private AnalyticsHttpResponse response(int status, String body) {
        return new AnalyticsHttpResponse(status, Map.of("content-type", List.of("text/plain")), body);
    }

    private MixpanelClient client(String secret, AnalyticsHttpTransport transport) {
        return new MixpanelClient("https://data.mixpanel.com", secret, UTC, List.of(),
                transport, new ObjectMapper());
    }

    /** properties.time 은 프로젝트 타임존 기준 epoch 초다. */
    private String event(String name, String user, String date, int hour) {
        long epoch = LocalDate.parse(date).atStartOfDay(UTC).plusHours(hour).toEpochSecond();
        return "{\"event\":\"" + name + "\",\"properties\":{\"time\":" + epoch
                + ",\"distinct_id\":\"" + user + "\"}}";
    }

    @Test
    @DisplayName("순서대로 밟은 고유 사용자를 세고 단계별 이탈을 낸다")
    void countsUsersWhoWalkStepsInOrder() {
        var transport = new StubTransport(response(200, String.join("\n",
                // 끝까지 간 사용자
                event("battle_step", "u1", "2026-08-02", 1),
                event("screen_view", "u1", "2026-08-02", 2),
                event("community_action", "u1", "2026-08-03", 3),
                // 2단계에서 멈춘 사용자
                event("battle_step", "u2", "2026-08-02", 1),
                event("screen_view", "u2", "2026-08-02", 5),
                // 1단계에서 멈춘 사용자
                event("battle_step", "u3", "2026-08-04", 1))));

        var result = client("secret", transport).fetchFunnel(STEPS, 7, from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.CONNECTED);
        assertThat(result.enteredUsers()).isEqualTo(3);
        assertThat(result.completedUsers()).isEqualTo(1);
        assertThat(result.conversionRate()).isEqualTo(33.33);
        assertThat(result.steps()).extracting(MixpanelFunnelReport.Step::event,
                                              MixpanelFunnelReport.Step::users,
                                              MixpanelFunnelReport.Step::droppedFromPrevious)
                .containsExactly(tuple("battle_step", 3L, null),
                                 tuple("screen_view", 2L, 1L),
                                 tuple("community_action", 1L, 1L));
        assertThat(result.steps().getLast().dropOffFromPrevious()).isEqualTo(50.00);
    }

    @Test
    @DisplayName("다음 단계가 앞 단계보다 먼저 일어났으면 전환으로 세지 않는다")
    void ignoresStepsOutOfOrder() {
        var transport = new StubTransport(response(200, String.join("\n",
                event("screen_view", "u1", "2026-08-02", 1),
                event("battle_step", "u1", "2026-08-02", 5))));

        var result = client("secret", transport).fetchFunnel(STEPS, 7, from, to);

        assertThat(result.enteredUsers()).isEqualTo(1);
        assertThat(result.steps().get(1).users()).isZero();
    }

    @Test
    @DisplayName("전환 윈도우를 넘겨 밟은 단계는 세지 않는다")
    void ignoresStepsAfterConversionWindow() {
        var transport = new StubTransport(response(200, String.join("\n",
                event("battle_step", "u1", "2026-08-01", 1),
                event("screen_view", "u1", "2026-08-20", 1))));

        var result = client("secret", transport).fetchFunnel(STEPS, 7, from, to);

        assertThat(result.enteredUsers()).isEqualTo(1);
        assertThat(result.steps().get(1).users()).isZero();
        assertThat(result.conversionRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("같은 단계를 여러 번 밟아도 한 사람으로 센다")
    void countsRepeatedStepsOnce() {
        var transport = new StubTransport(response(200, String.join("\n",
                event("battle_step", "u1", "2026-08-02", 1),
                event("battle_step", "u1", "2026-08-02", 2),
                event("screen_view", "u1", "2026-08-02", 3),
                event("screen_view", "u1", "2026-08-02", 4))));

        var result = client("secret", transport).fetchFunnel(STEPS, 7, from, to);

        assertThat(result.enteredUsers()).isEqualTo(1);
        assertThat(result.steps().get(1).users()).isEqualTo(1);
    }

    @Test
    @DisplayName("아무도 진입하지 않으면 전환율은 0%가 아니라 null 이다")
    void leavesRatesNullWithoutEntry() {
        var transport = new StubTransport(response(200, event("point_action", "u1", "2026-08-02", 1)));

        var result = client("secret", transport).fetchFunnel(STEPS, 7, from, to);

        assertThat(result.enteredUsers()).isZero();
        assertThat(result.conversionRate()).isNull();
        assertThat(result.steps()).extracting(MixpanelFunnelReport.Step::conversionFromEntry)
                .containsOnlyNulls();
    }

    @Test
    @DisplayName("사용자를 가리지 못하는 이벤트는 고유 전환에서 제외한다")
    void skipsEventsWithoutDistinctId() {
        var transport = new StubTransport(response(200,
                "{\"event\":\"battle_step\",\"properties\":{\"time\":1786000000}}"));

        var result = client("secret", transport).fetchFunnel(STEPS, 7, from, to);

        assertThat(result.enteredUsers()).isZero();
    }

    /** 배틀 흐름은 선택·재생·투표가 모두 battle_step 으로 들어오고 step_name 으로만 갈린다. */
    private String battleStep(String user, String stepName, String date, int hour) {
        long epoch = LocalDate.parse(date).atStartOfDay(UTC).plusHours(hour).toEpochSecond();
        return "{\"event\":\"battle_step\",\"properties\":{\"time\":" + epoch
                + ",\"distinct_id\":\"" + user + "\",\"step_name\":\"" + stepName + "\"}}";
    }

    @Test
    @DisplayName("같은 이벤트를 속성으로 갈라 단계를 나눈다. 배틀 선택·재생·투표가 한 단계로 뭉치지 않는다")
    void splitsOneEventIntoStepsByProperty() {
        var steps = List.of("battle_step:step_name=pre_vote",
                            "battle_step:step_name=audio_end",
                            "battle_step:step_name=post_vote",
                            "community_action");
        var transport = new StubTransport(response(200, String.join("\n",
                // 끝까지 간 사용자
                battleStep("u1", "pre_vote", "2026-08-02", 1),
                battleStep("u1", "audio_end", "2026-08-02", 2),
                battleStep("u1", "post_vote", "2026-08-02", 3),
                event("community_action", "u1", "2026-08-02", 4),
                // 재생까지만 간 사용자
                battleStep("u2", "pre_vote", "2026-08-02", 1),
                battleStep("u2", "audio_end", "2026-08-02", 2),
                // 선택만 한 사용자
                battleStep("u3", "pre_vote", "2026-08-03", 1))));

        var result = client("secret", transport).fetchFunnel(steps, 7, from, to);

        assertThat(result.steps()).extracting(MixpanelFunnelReport.Step::event,
                                              MixpanelFunnelReport.Step::users)
                .containsExactly(tuple("battle_step:step_name=pre_vote", 3L),
                                 tuple("battle_step:step_name=audio_end", 2L),
                                 tuple("battle_step:step_name=post_vote", 1L),
                                 tuple("community_action", 1L));
        assertThat(result.conversionRate()).isEqualTo(33.33);
    }

    @Test
    @DisplayName("속성 값이 다른 이벤트는 그 단계로 세지 않는다")
    void ignoresEventsWithDifferentPropertyValue() {
        var steps = List.of("battle_step:step_name=pre_vote", "battle_step:step_name=post_vote");
        var transport = new StubTransport(response(200, String.join("\n",
                battleStep("u1", "pre_vote", "2026-08-02", 1),
                battleStep("u1", "audio_end", "2026-08-02", 2))));

        var result = client("secret", transport).fetchFunnel(steps, 7, from, to);

        assertThat(result.enteredUsers()).isEqualTo(1);
        assertThat(result.steps().getLast().users()).isZero();
    }

    @Test
    @DisplayName("단계 속성 문법이 잘못되면 UNAVAILABLE 로 알린다. 조용히 0명으로 두지 않는다")
    void reportsUnavailableOnMalformedStepSpec() {
        var transport = new StubTransport(response(200, battleStep("u1", "pre_vote", "2026-08-02", 1)));

        var result = client("secret", transport)
                .fetchFunnel(List.of("battle_step:step_name", "community_action"), 7, from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.UNAVAILABLE);
    }

    @Test
    @DisplayName("API 비밀이 없으면 호출하지 않고 NOT_CONFIGURED 를 준다")
    void returnsNotConfiguredWithoutSecret() {
        var transport = new StubTransport(response(200, ""));

        var result = client("", transport).fetchFunnel(STEPS, 7, from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.NOT_CONFIGURED);
        assertThat(result.steps()).extracting(MixpanelFunnelReport.Step::event)
                .containsExactlyElementsOf(STEPS);
        assertThat(transport.uri).isNull();
    }

    @Test
    @DisplayName("플랜 제한이나 자격 거절이면 UNAVAILABLE 이다. 0명으로 보여주지 않는다")
    void reportsUnavailableOnRejectedRequest() {
        var transport = new StubTransport(response(402, "{\"error\":\"plan\"}"));

        var result = client("secret", transport).fetchFunnel(STEPS, 7, from, to);

        assertThat(result.status()).isEqualTo(AnalyticsStatus.UNAVAILABLE);
        assertThat(result.enteredUsers()).isNull();
        assertThat(result.steps()).extracting(MixpanelFunnelReport.Step::users).containsOnlyNulls();
    }

    @Test
    @DisplayName("2026-08 실데이터 기준 Mixpanel 화면 숫자와 같은 규칙으로 센다")
    void matchesMixpanelConsoleSemantics() {
        // Mixpanel 화면: battle_step 24 → screen_view 24 → community_action 2 (8.33%).
        // 같은 규칙인지 확인하려고 24명 중 2명만 마지막 단계를 밟은 모양을 만든다.
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < 24; index++) {
            String user = "u" + index;
            lines.add(event("battle_step", user, "2026-08-05", 1));
            lines.add(event("screen_view", user, "2026-08-05", 2));
            if (index < 2) {
                lines.add(event("community_action", user, "2026-08-06", 3));
            }
        }
        var transport = new StubTransport(response(200, String.join("\n", lines)));

        var result = client("secret", transport).fetchFunnel(STEPS, 7, from, to);

        assertThat(result.steps()).extracting(MixpanelFunnelReport.Step::users)
                .containsExactly(24L, 24L, 2L);
        assertThat(result.conversionRate()).isEqualTo(8.33);
    }

    private String raw(String name, String date, int hour, int minute, String props) {
        long epoch = LocalDate.parse(date).atStartOfDay(UTC).plusHours(hour).plusMinutes(minute).toEpochSecond();
        return "{\"event\":\"" + name + "\",\"properties\":{\"time\":" + epoch + "," + props + "}}";
    }

    @Test
    @DisplayName("groupBy=class_id 면 단계마다 사람이 달라도 클래스 단위로 이어서 세고, 단계 사이 시간 중앙값을 낸다")
    void groupsByClassAndMeasuresStepDurations() {
        List<String> steps = List.of("class_create_complete", "class_join_complete", "class_vote_post");
        var transport = new StubTransport(response(200, String.join("\n",
                // 클래스 1: 운영자 o1 개설 → 참가자 p1 입장 10분 뒤 → p1 사후투표 30분 뒤
                raw("class_create_complete", "2026-08-02", 1, 0, "\"distinct_id\":\"o1\",\"class_id\":1"),
                raw("class_join_complete", "2026-08-02", 1, 10, "\"distinct_id\":\"p1\",\"class_id\":1"),
                raw("class_vote_post", "2026-08-02", 1, 40, "\"distinct_id\":\"p1\",\"class_id\":1"),
                // 클래스 2: 개설 → 입장 20분 뒤, 투표 없음
                raw("class_create_complete", "2026-08-03", 1, 0, "\"distinct_id\":\"o2\",\"class_id\":2"),
                raw("class_join_complete", "2026-08-03", 1, 20, "\"distinct_id\":\"p2\",\"class_id\":2"),
                // class_id 없는 이벤트는 빠진다
                raw("class_create_complete", "2026-08-03", 2, 0, "\"distinct_id\":\"o3\""))));

        var result = client("secret", transport).fetchFunnel(steps, 7, from, to, "class_id");

        assertThat(result.groupBy()).isEqualTo("class_id");
        assertThat(result.steps()).extracting(MixpanelFunnelReport.Step::users,
                                              MixpanelFunnelReport.Step::medianSecondsFromPrevious)
                .containsExactly(tuple(2L, null), tuple(2L, 900L), tuple(1L, 1800L));
        assertThat(result.medianSecondsToComplete()).isEqualTo(2400L);

        // 사용자 기준이면 운영자와 참가자가 달라 개설 다음 단계로 이어지지 않는다
        var byUser = client("secret", new StubTransport(response(200, String.join("\n",
                raw("class_create_complete", "2026-08-02", 1, 0, "\"distinct_id\":\"o1\",\"class_id\":1"),
                raw("class_join_complete", "2026-08-02", 1, 10, "\"distinct_id\":\"p1\",\"class_id\":1")))))
                .fetchFunnel(steps, 7, from, to);
        assertThat(byUser.steps().get(1).users()).isZero();
    }

    @Test
    @DisplayName("단계 조건은 ';' 로 여러 개, '~' 로 정규식 전체 일치를 쓸 수 있다 (network_request 경로 매칭)")
    void matchesMultipleConditionsAndRegex() {
        List<String> steps = List.of(
                "network_request:method=POST;path=/api/v1/classes/join",
                "network_request:method=POST;path~/api/v1/classes/\\d+/battle/votes/post");
        var transport = new StubTransport(response(200, String.join("\n",
                raw("network_request", "2026-08-02", 1, 0, "\"distinct_id\":\"u1\",\"method\":\"POST\",\"path\":\"/api/v1/classes/join\""),
                raw("network_request", "2026-08-02", 1, 5, "\"distinct_id\":\"u1\",\"method\":\"GET\",\"path\":\"/api/v1/classes/31/battle/votes/post\""),
                raw("network_request", "2026-08-02", 1, 9, "\"distinct_id\":\"u1\",\"method\":\"POST\",\"path\":\"/api/v1/classes/31/battle/votes/post\""),
                raw("network_request", "2026-08-02", 2, 0, "\"distinct_id\":\"u2\",\"method\":\"POST\",\"path\":\"/api/v1/classes/join\""),
                raw("network_request", "2026-08-02", 2, 1, "\"distinct_id\":\"u2\",\"method\":\"POST\",\"path\":\"/api/v1/classes/31/battle/votes/post/extra\""))));

        var result = client("secret", transport).fetchFunnel(steps, 7, from, to);

        assertThat(result.steps()).extracting(MixpanelFunnelReport.Step::users,
                                              MixpanelFunnelReport.Step::medianSecondsFromPrevious)
                .containsExactly(tuple(2L, null), tuple(1L, 540L));
    }

    @Test
    @DisplayName("중앙값: 짝수 개면 가운데 두 값의 평균, 값이 없으면 null")
    void median() {
        assertThat(MixpanelClient.median(List.of(30L, 10L, 20L))).isEqualTo(20L);
        assertThat(MixpanelClient.median(List.of(10L, 20L, 30L, 40L))).isEqualTo(25L);
        assertThat(MixpanelClient.median(List.of())).isNull();
    }
}
