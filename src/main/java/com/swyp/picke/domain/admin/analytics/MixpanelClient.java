package com.swyp.picke.domain.admin.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Mixpanel 원본 이벤트를 내려받아 서버가 일별로 센다.
 *
 * <p>집계 API(segmentation·insights)를 쓰지 않는다. 현재 프로젝트 플랜이 그쪽을 막아
 * {@code HTTP 402 Your plan does not allow API calls} 를 준다. 반면 Raw Export
 * ({@code data.mixpanel.com/api/2.0/export}) 는 같은 플랜에서 열려 있어 여기서 직접 센다.
 *
 * <p>인증은 프로젝트 API 비밀을 Basic 사용자명 자리에 넣는 레거시 방식이다.
 * 이 방식에는 {@code project_id} 파라미터를 넣으면 400 이 되므로 넣지 않는다.
 * 비밀이 이미 프로젝트를 특정한다.
 *
 * <p>응답은 한 줄에 이벤트 하나인 NDJSON 이다. {@code properties.time} 은 프로젝트 타임존
 * 기준 epoch 초이며, 요청의 {@code from_date}·{@code to_date} 경계도 같은 타임존을 따른다.
 * 둘을 같은 타임존으로 묶어야 날짜 버킷이 Mixpanel 화면 숫자와 맞는다.
 */
@Slf4j
@Component
public class MixpanelClient {

    /** 원본 이벤트를 전부 받으므로 기간이 넓으면 응답이 급격히 커진다. */
    static final long MAX_DAYS = 31;

    private final String baseUrl;
    private final String apiSecret;
    private final ZoneId projectZone;
    private final AnalyticsHttpTransport transport;
    private final ObjectMapper objectMapper;

    @Autowired
    public MixpanelClient(
            @Value("${picke.analytics.mixpanel.base-url:https://data.mixpanel.com}") String baseUrl,
            @Value("${picke.analytics.mixpanel.api-secret:${MIXPANEL_API_SECRET:}}") String apiSecret,
            @Value("${picke.analytics.mixpanel.project-zone:UTC}") String projectZone,
            @Value("${picke.analytics.mixpanel.default-events:}") List<String> defaultEvents,
            AnalyticsHttpTransport transport) {
        this(baseUrl, apiSecret, ZoneId.of(projectZone), defaultEvents, transport, new ObjectMapper());
    }

    MixpanelClient(String baseUrl, String apiSecret, ZoneId projectZone, List<String> defaultEvents,
                   AnalyticsHttpTransport transport, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.apiSecret = apiSecret;
        this.projectZone = projectZone;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    /**
     * @param requestedEvents 빈 값이면 기간에 실제로 나타난 이벤트 전부를 센다.
     */
    public MixpanelEventReport fetchDailyCounts(List<String> requestedEvents, LocalDate from, LocalDate to) {
        if (!StringUtils.hasText(apiSecret)) {
            return MixpanelEventReport.empty(AnalyticsStatus.NOT_CONFIGURED, from, to);
        }

        AnalyticsHttpResponse response = transport.get(uri(from, to),
                Map.of("Authorization", "Basic " + basicCredentials()));
        if (!response.isSuccess()) {
            log.warn("[Mixpanel] 원본 이벤트 조회 실패: status={}", response.statusCode());
            return MixpanelEventReport.empty(AnalyticsStatus.UNAVAILABLE, from, to);
        }

        try {
            Aggregation aggregation = aggregate(response.body(), targetEvents(requestedEvents), from, to);
            return new MixpanelEventReport(AnalyticsStatus.CONNECTED, Instant.now(), from, to,
                    aggregation.totalEvents(), aggregation.uniqueUsers(), to,
                    aggregation.activeUsers(), aggregation.signUps(), aggregation.signUpDays(),
                    aggregation.availableEvents(), aggregation.events());
        } catch (Exception e) {
            log.warn("[Mixpanel] 원본 이벤트 파싱 실패: {}", e.getClass().getSimpleName());
            return MixpanelEventReport.empty(AnalyticsStatus.UNAVAILABLE, from, to);
        }
    }

    private List<String> targetEvents(List<String> requestedEvents) {
        if (requestedEvents != null && !requestedEvents.isEmpty()) {
            return requestedEvents;
        }
        return List.of();
    }

    /**
     * NDJSON 을 한 줄씩 세어 이벤트×날짜 표를 만든다.
     *
     * <p>지정한 이벤트가 없으면 기간에 나타난 이벤트를 전부 센다. 원본을 받았으므로
     * 어떤 이벤트가 있었는지 서버가 알 수 있다. 이름을 미리 설정해 둘 필요가 없다.
     */
    private Aggregation aggregate(
            String body, List<String> targets, LocalDate from, LocalDate to) throws Exception {
        Set<String> wanted = targets.isEmpty() ? null : new LinkedHashSet<>(targets);
        Map<String, EventStats> stats = new HashMap<>();
        Set<String> availableEvents = new HashSet<>();
        Set<String> reportUsers = new HashSet<>();
        Set<String> activeUsers = new HashSet<>();
        Map<LocalDate, Long> signUpsByDate = new HashMap<>();
        long signUps = 0;

        for (String line : body.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            JsonNode node = objectMapper.readTree(line);
            String event = node.path("event").asText(null);
            JsonNode time = node.path("properties").path("time");
            if (event == null || !time.isNumber()) {
                throw new IllegalArgumentException("Mixpanel export line is missing event or time.");
            }
            availableEvents.add(event);
            Instant occurredAt = Instant.ofEpochSecond(time.asLong());
            LocalDate date = occurredAt.atZone(projectZone).toLocalDate();
            if (date.isBefore(from) || date.isAfter(to)) {
                // 경계 하루가 타임존 차이로 걸쳐 들어올 수 있다. 요청 기간 밖은 버린다.
                continue;
            }
            String distinctId = text(node.path("properties").get("distinct_id"));
            if (date.equals(to)) {
                if (distinctId != null) {
                    activeUsers.add(distinctId);
                }
                if ("sign_up".equals(event)) {
                    signUps++;
                }
            }
            if ("sign_up".equals(event)) {
                signUpsByDate.merge(date, 1L, Long::sum);
            }
            if (wanted != null && !wanted.contains(event)) {
                continue;
            }
            EventStats eventStats = stats.computeIfAbsent(event, key -> new EventStats());
            eventStats.add(date, occurredAt, distinctId);
            if (distinctId != null) {
                reportUsers.add(distinctId);
            }
        }

        List<String> events = wanted != null ? List.copyOf(wanted) : new ArrayList<>(stats.keySet());
        List<MixpanelEventReport.EventSeries> series = events.stream()
                .map(event -> series(event, stats.getOrDefault(event, new EventStats()), from, to))
                .sorted(Comparator.comparing(MixpanelEventReport.EventSeries::total).reversed())
                .toList();
        long total = series.stream().mapToLong(MixpanelEventReport.EventSeries::total).sum();
        return new Aggregation(total, (long) reportUsers.size(), (long) activeUsers.size(), signUps,
                signUpDays(signUpsByDate, from, to),
                availableEvents.stream().sorted().toList(), series);
    }

    private List<MixpanelEventReport.SignUpDay> signUpDays(
            Map<LocalDate, Long> counts, LocalDate from, LocalDate to) {
        List<MixpanelEventReport.SignUpDay> days = new ArrayList<>();
        for (LocalDate cursor = from; !cursor.isAfter(to); cursor = cursor.plusDays(1)) {
            days.add(new MixpanelEventReport.SignUpDay(cursor, counts.getOrDefault(cursor, 0L)));
        }
        return days;
    }

    /** 원본을 전부 받았으므로 이벤트가 없던 날짜는 미집계가 아니라 0 이다. */
    private MixpanelEventReport.EventSeries series(
            String event, EventStats stats, LocalDate from, LocalDate to) {
        List<MixpanelEventReport.Day> days = new ArrayList<>();
        for (LocalDate cursor = to; !cursor.isBefore(from); cursor = cursor.minusDays(1)) {
            days.add(new MixpanelEventReport.Day(
                    cursor,
                    stats.counts.getOrDefault(cursor, 0L),
                    (long) stats.users.getOrDefault(cursor, Set.of()).size()));
        }
        return new MixpanelEventReport.EventSeries(
                event, stats.total, (long) stats.allUsers.size(), stats.firstSeen, stats.lastSeen, days);
    }

    private String text(JsonNode value) {
        return value == null || value.isNull() || value.isContainerNode() ? null : value.asText();
    }

    private record Aggregation(
            Long totalEvents,
            Long uniqueUsers,
            Long activeUsers,
            Long signUps,
            List<MixpanelEventReport.SignUpDay> signUpDays,
            List<String> availableEvents,
            List<MixpanelEventReport.EventSeries> events) {
    }

    private static final class EventStats {
        private final Map<LocalDate, Long> counts = new HashMap<>();
        private final Map<LocalDate, Set<String>> users = new HashMap<>();
        private final Set<String> allUsers = new HashSet<>();
        private long total;
        private Instant firstSeen;
        private Instant lastSeen;

        private void add(LocalDate date, Instant occurredAt, String distinctId) {
            counts.merge(date, 1L, Long::sum);
            total++;
            if (distinctId != null) {
                allUsers.add(distinctId);
                users.computeIfAbsent(date, key -> new HashSet<>()).add(distinctId);
            }
            if (firstSeen == null || occurredAt.isBefore(firstSeen)) {
                firstSeen = occurredAt;
            }
            if (lastSeen == null || occurredAt.isAfter(lastSeen)) {
                lastSeen = occurredAt;
            }
        }
    }

    /**
     * 지정한 순서대로 이벤트를 밟은 고유 사용자를 센다.
     *
     * <p>집계 API 의 퍼널 리포트를 쓸 수 없어(플랜 제한) 원본 이벤트로 직접 계산한다.
     * 규칙은 Mixpanel 화면과 같다. 다음 단계는 앞 단계 이후여야 하고, 첫 단계로부터
     * {@code windowDays} 안에 끝나야 한다. 2026-08 실데이터로 Mixpanel 화면 숫자와 일치를 확인했다.
     */
    public MixpanelFunnelReport fetchFunnel(
            List<String> steps, int windowDays, LocalDate from, LocalDate to) {
        if (!StringUtils.hasText(apiSecret)) {
            return MixpanelFunnelReport.empty(AnalyticsStatus.NOT_CONFIGURED, steps, windowDays, from, to);
        }

        AnalyticsHttpResponse response = transport.get(uri(from, to),
                Map.of("Authorization", "Basic " + basicCredentials()));
        if (!response.isSuccess()) {
            log.warn("[Mixpanel] 퍼널용 원본 이벤트 조회 실패: status={}", response.statusCode());
            return MixpanelFunnelReport.empty(AnalyticsStatus.UNAVAILABLE, steps, windowDays, from, to);
        }

        try {
            long[] reached = walkFunnel(response.body(), steps, windowDays, from, to);
            return funnelReport(steps, windowDays, from, to, reached);
        } catch (Exception e) {
            log.warn("[Mixpanel] 퍼널 계산 실패: {}", e.getClass().getSimpleName());
            return MixpanelFunnelReport.empty(AnalyticsStatus.UNAVAILABLE, steps, windowDays, from, to);
        }
    }

    /**
     * 단계 하나를 가리키는 조건. {@code battle_step} 처럼 이벤트 이름만 쓰거나
     * {@code battle_step:step_name=pre_vote} 처럼 속성까지 좁힌다.
     *
     * <p>배틀 흐름은 선택·재생·투표가 모두 {@code battle_step} 한 이벤트로 들어오고
     * {@code step_name} 으로만 갈린다. 속성을 못 좁히면 이 구간이 한 단계로 뭉쳐 이탈율을 볼 수 없다.
     */
    private record StepMatcher(String spec, String event, String property, String value) {
        static StepMatcher parse(String spec) {
            int marker = spec.indexOf(':');
            if (marker < 0) {
                return new StepMatcher(spec, spec, null, null);
            }
            String event = spec.substring(0, marker);
            String filter = spec.substring(marker + 1);
            int equals = filter.indexOf('=');
            if (equals < 0) {
                throw new IllegalArgumentException("퍼널 단계 속성은 property=value 형태여야 합니다: " + spec);
            }
            return new StepMatcher(spec, event, filter.substring(0, equals), filter.substring(equals + 1));
        }

        boolean matches(String event, JsonNode properties) {
            if (!this.event.equals(event)) {
                return false;
            }
            if (property == null) {
                return true;
            }
            JsonNode actual = properties.get(property);
            return actual != null && !actual.isNull() && value.equals(actual.asText());
        }
    }

    /** 사용자별로 단계 이벤트만 모아 시간순으로 훑는다. 단계에 없는 이벤트는 담지 않아 메모리를 아낀다. */
    private long[] walkFunnel(
            String body, List<String> steps, int windowDays, LocalDate from, LocalDate to) throws Exception {
        List<StepMatcher> matchers = steps.stream().map(StepMatcher::parse).toList();
        Map<String, List<long[]>> byUser = new HashMap<>();

        for (String line : body.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            JsonNode node = objectMapper.readTree(line);
            String event = node.path("event").asText(null);
            JsonNode time = node.path("properties").path("time");
            if (event == null || !time.isNumber()) {
                throw new IllegalArgumentException("Mixpanel export line is missing event or time.");
            }
            JsonNode properties = node.path("properties");
            int step = -1;
            for (int index = 0; index < matchers.size(); index++) {
                if (matchers.get(index).matches(event, properties)) {
                    step = index;
                    break;
                }
            }
            if (step < 0) {
                continue;
            }
            Instant occurredAt = Instant.ofEpochSecond(time.asLong());
            LocalDate date = occurredAt.atZone(projectZone).toLocalDate();
            if (date.isBefore(from) || date.isAfter(to)) {
                continue;
            }
            String distinctId = text(properties.get("distinct_id"));
            if (distinctId == null) {
                // 사용자를 못 가리는 이벤트는 고유 전환을 셀 수 없다.
                continue;
            }
            byUser.computeIfAbsent(distinctId, key -> new ArrayList<>())
                    .add(new long[]{time.asLong(), step});
        }

        long[] reached = new long[steps.size()];
        long window = (long) windowDays * 86_400L;
        for (List<long[]> events : byUser.values()) {
            events.sort(Comparator.comparingLong(entry -> entry[0]));
            Long entry = firstTimeOf(events, 0);
            if (entry == null) {
                continue;
            }
            reached[0]++;
            long cursor = entry;
            long deadline = entry + window;
            for (int step = 1; step < steps.size(); step++) {
                Long next = nextTimeOf(events, step, cursor, deadline);
                if (next == null) {
                    break;
                }
                reached[step]++;
                cursor = next;
            }
        }
        return reached;
    }

    private Long firstTimeOf(List<long[]> events, int step) {
        return nextTimeOf(events, step, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private Long nextTimeOf(List<long[]> events, int step, long notBefore, long notAfter) {
        for (long[] entry : events) {
            if (entry[1] == step && entry[0] >= notBefore && entry[0] <= notAfter) {
                return entry[0];
            }
        }
        return null;
    }

    private MixpanelFunnelReport funnelReport(
            List<String> steps, int windowDays, LocalDate from, LocalDate to, long[] reached) {
        long entered = reached[0];
        List<MixpanelFunnelReport.Step> rows = new ArrayList<>();
        for (int index = 0; index < steps.size(); index++) {
            long users = reached[index];
            Long previous = index == 0 ? null : reached[index - 1];
            rows.add(new MixpanelFunnelReport.Step(
                    index + 1,
                    steps.get(index),
                    users,
                    percentage(users, entered),
                    previous == null ? null : percentage(users, previous),
                    previous == null ? null : previous - users,
                    previous == null ? null : percentage(previous - users, previous)));
        }
        long completed = reached[reached.length - 1];
        return new MixpanelFunnelReport(AnalyticsStatus.CONNECTED, Instant.now(), from, to, windowDays,
                entered, completed, percentage(completed, entered), rows);
    }

    /** 분모가 0이면 0%가 아니라 null 이다. 아무도 진입하지 않은 것과 전환 실패를 구분한다. */
    private Double percentage(long value, long total) {
        if (total <= 0) {
            return null;
        }
        return BigDecimal.valueOf(value * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String basicCredentials() {
        // 레거시 방식은 API 비밀을 사용자명 자리에 두고 비밀번호를 비운다.
        return Base64.getEncoder()
                .encodeToString((apiSecret + ":").getBytes(StandardCharsets.UTF_8));
    }

    private URI uri(LocalDate from, LocalDate to) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/2.0/export")
                .queryParam("from_date", from)
                .queryParam("to_date", to)
                .build()
                .toUri();
    }
}
