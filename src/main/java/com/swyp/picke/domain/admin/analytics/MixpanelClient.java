package com.swyp.picke.domain.admin.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Mixpanel Query API 로 이벤트 일별 발생 수를 읽는다.
 *
 * <p>인증은 서비스 계정 Basic 이며 모든 요청에 project_id 가 필요하다.
 * segmentation 은 Mixpanel 이 유지보수 모드로 둔 엔드포인트지만, 저장된 리포트(bookmark)를
 * 미리 만들어 둘 필요가 없어 관리자 화면에서 날짜만 바꿔 조회하는 용도에 맞다.
 *
 * <p>이벤트 이름은 서버가 알 수 없다. 앱이 무엇을 트래킹하는지에 달렸으므로 설정이나 요청으로 받는다.
 */
@Slf4j
@Component
public class MixpanelClient {

    private final String baseUrl;
    private final String projectId;
    private final String serviceAccountUsername;
    private final String serviceAccountSecret;
    private final List<String> defaultEvents;
    private final AnalyticsHttpTransport transport;
    private final ObjectMapper objectMapper;

    @Autowired
    public MixpanelClient(
            @Value("${picke.analytics.mixpanel.base-url:https://mixpanel.com}") String baseUrl,
            @Value("${picke.analytics.mixpanel.project-id:${MIXPANEL_PROJECT_ID:}}") String projectId,
            @Value("${picke.analytics.mixpanel.service-account-username:${MIXPANEL_SERVICE_ACCOUNT_USERNAME:}}")
            String serviceAccountUsername,
            @Value("${picke.analytics.mixpanel.service-account-secret:${MIXPANEL_SERVICE_ACCOUNT_SECRET:}}")
            String serviceAccountSecret,
            @Value("${picke.analytics.mixpanel.default-events:}") List<String> defaultEvents,
            AnalyticsHttpTransport transport) {
        this(baseUrl, projectId, serviceAccountUsername, serviceAccountSecret, defaultEvents, transport,
                new ObjectMapper());
    }

    MixpanelClient(String baseUrl, String projectId, String serviceAccountUsername,
                   String serviceAccountSecret, List<String> defaultEvents,
                   AnalyticsHttpTransport transport, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.projectId = projectId;
        this.serviceAccountUsername = serviceAccountUsername;
        this.serviceAccountSecret = serviceAccountSecret;
        this.defaultEvents = defaultEvents;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    public MixpanelEventReport fetchDailyCounts(List<String> requestedEvents, LocalDate from, LocalDate to) {
        List<String> events = requestedEvents == null || requestedEvents.isEmpty()
                ? defaultEvents
                : requestedEvents;
        if (!isConfigured() || events == null || events.isEmpty()) {
            return MixpanelEventReport.empty(AnalyticsStatus.NOT_CONFIGURED, from, to);
        }

        List<MixpanelEventReport.EventSeries> series = new ArrayList<>();
        for (String event : events) {
            MixpanelEventReport.EventSeries fetched = fetchOne(event, from, to);
            if (fetched == null) {
                return MixpanelEventReport.empty(AnalyticsStatus.UNAVAILABLE, from, to);
            }
            series.add(fetched);
        }
        return new MixpanelEventReport(AnalyticsStatus.CONNECTED, Instant.now(), from, to, series);
    }

    /** 한 이벤트라도 못 읽으면 null 을 준다. 일부만 성공한 지표를 전체 합계처럼 보여주지 않는다. */
    private MixpanelEventReport.EventSeries fetchOne(String event, LocalDate from, LocalDate to) {
        AnalyticsHttpResponse response = transport.get(uri(event, from, to),
                Map.of("Authorization", "Basic " + basicCredentials()));
        if (!response.isSuccess() || !response.isJson()) {
            log.warn("[Mixpanel] 이벤트 조회 실패: event={}, status={}", event, response.statusCode());
            return null;
        }

        try {
            return parseSeries(event, response.body(), from, to);
        } catch (Exception e) {
            log.warn("[Mixpanel] 응답 파싱 실패: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    private boolean isConfigured() {
        return StringUtils.hasText(projectId)
                && StringUtils.hasText(serviceAccountUsername)
                && StringUtils.hasText(serviceAccountSecret);
    }

    private String basicCredentials() {
        String raw = serviceAccountUsername + ":" + serviceAccountSecret;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private URI uri(String event, LocalDate from, LocalDate to) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/query/segmentation")
                .queryParam("project_id", projectId)
                .queryParam("event", event)
                .queryParam("from_date", from)
                .queryParam("to_date", to)
                .queryParam("unit", "day")
                .build()
                .encode()
                .toUri();
    }

    /** 응답은 {@code {data:{series:[날짜...], values:{이벤트:{날짜:건수}}}}} 형태다. */
    private MixpanelEventReport.EventSeries parseSeries(String event, String body, LocalDate from, LocalDate to)
            throws Exception {
        JsonNode values = objectMapper.readTree(body).path("data").path("values").path(event);
        if (values.isMissingNode()) {
            throw new IllegalArgumentException("Mixpanel response has no values for the event.");
        }

        List<MixpanelEventReport.Day> days = new ArrayList<>();
        long total = 0;
        for (LocalDate cursor = to; !cursor.isBefore(from); cursor = cursor.minusDays(1)) {
            JsonNode count = values.get(cursor.toString());
            if (count == null || count.isNull()) {
                days.add(new MixpanelEventReport.Day(cursor, null));
                continue;
            }
            days.add(new MixpanelEventReport.Day(cursor, count.asLong()));
            total += count.asLong();
        }
        return new MixpanelEventReport.EventSeries(event, total, days);
    }
}
