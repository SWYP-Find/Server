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
 * <p>인증: 서비스 계정(사용자명·비밀·프로젝트 ID)이 설정돼 있으면 그것을 쓰고 {@code project_id} 를 붙인다.
 * 없으면 프로젝트 API 비밀을 Basic 사용자명 자리에 넣는 레거시 방식을 쓴다 (이때 {@code project_id} 를 넣으면 400).
 * 프로젝트 비밀은 2027-03-03 제거 예정이라 서비스 계정으로 옮겨야 한다 (#687).
 *
 * <p>응답은 한 줄에 이벤트 하나인 NDJSON 이다. {@code properties.time} 은 실제 UTC epoch 초이고,
 * {@code from_date}·{@code to_date} 는 프로젝트 시간대 설정과 관계없이 UTC 날짜로 잘린다 (2026-11-24 실측).
 * 그래서 요청은 앞뒤로 하루 넓혀 받고, 날짜는 {@code projectZone}(기본 Asia/Seoul) 기준으로 다시 자른다 (#687).
 */
@Slf4j
@Component
public class MixpanelClient {

    /** 원본 이벤트를 전부 받으므로 기간이 넓으면 응답이 급격히 커진다. */
    static final long MAX_DAYS = 31;

    private final String baseUrl;
    private final String apiSecret;
    private final String serviceAccountUsername;
    private final String serviceAccountSecret;
    private final String projectId;
    private final ZoneId projectZone;
    private final AnalyticsHttpTransport transport;
    private final ObjectMapper objectMapper;

    @Autowired
    public MixpanelClient(
            @Value("${picke.analytics.mixpanel.base-url:https://data.mixpanel.com}") String baseUrl,
            @Value("${picke.analytics.mixpanel.api-secret:${MIXPANEL_API_SECRET:}}") String apiSecret,
            @Value("${picke.analytics.mixpanel.service-account-username:${MIXPANEL_SERVICE_ACCOUNT_USERNAME:}}")
            String serviceAccountUsername,
            @Value("${picke.analytics.mixpanel.service-account-secret:${MIXPANEL_SERVICE_ACCOUNT_SECRET:}}")
            String serviceAccountSecret,
            @Value("${picke.analytics.mixpanel.project-id:${MIXPANEL_PROJECT_ID:}}") String projectId,
            @Value("${picke.analytics.mixpanel.project-zone:Asia/Seoul}") String projectZone,
            @Value("${picke.analytics.mixpanel.default-events:}") List<String> defaultEvents,
            AnalyticsHttpTransport transport) {
        this(baseUrl, apiSecret, serviceAccountUsername, serviceAccountSecret, projectId,
                ZoneId.of(projectZone), defaultEvents, transport, new ObjectMapper());
    }

    MixpanelClient(String baseUrl, String apiSecret, ZoneId projectZone, List<String> defaultEvents,
                   AnalyticsHttpTransport transport, ObjectMapper objectMapper) {
        this(baseUrl, apiSecret, null, null, null, projectZone, defaultEvents, transport, objectMapper);
    }

    MixpanelClient(String baseUrl, String apiSecret, String serviceAccountUsername, String serviceAccountSecret,
                   String projectId, ZoneId projectZone, List<String> defaultEvents,
                   AnalyticsHttpTransport transport, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.apiSecret = apiSecret;
        this.serviceAccountUsername = serviceAccountUsername;
        this.serviceAccountSecret = serviceAccountSecret;
        this.projectId = projectId;
        this.projectZone = projectZone;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    /**
     * @param requestedEvents 빈 값이면 기간에 실제로 나타난 이벤트 전부를 센다.
     */
    public MixpanelEventReport fetchDailyCounts(List<String> requestedEvents, LocalDate from, LocalDate to) {
        if (!isConfigured()) {
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
        Set<String> seen = new HashSet<>();

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
            if (isDuplicate(seen, event, node.path("properties"))) {
                continue;
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

    /**
     * Raw Export 는 같은 이벤트를 여러 줄로 돌려줄 수 있다 (실측: 같은 $insert_id 가 보통 5줄, #729).
     * Mixpanel 화면과 같은 규칙으로 이벤트 이름·distinct_id·time·$insert_id 가 모두 같으면 한 번만 센다.
     * $insert_id 가 없는 줄은 구분할 수 없어 그대로 센다.
     */
    private boolean isDuplicate(Set<String> seen, String event, JsonNode properties) {
        String insertId = text(properties.get("$insert_id"));
        if (insertId == null) {
            return false;
        }
        String key = event + '\u0001' + text(properties.get("distinct_id")) + '\u0001'
                + properties.path("time").asText() + '\u0001' + insertId;
        return !seen.add(key);
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

    /** 사용자(distinct_id) 기준 퍼널. {@link #fetchFunnel(List, int, LocalDate, LocalDate, String)} 참고 */
    public MixpanelFunnelReport fetchFunnel(
            List<String> steps, int windowDays, LocalDate from, LocalDate to) {
        return fetchFunnel(steps, windowDays, from, to, null);
    }

    /**
     * 지정한 순서대로 이벤트를 밟은 고유 대상(기본 사용자)을 센다.
     *
     * <p>집계 API 의 퍼널 리포트를 쓸 수 없어(플랜 제한) 원본 이벤트로 직접 계산한다.
     * 규칙은 Mixpanel 화면과 같다. 다음 단계는 앞 단계 이후여야 하고, 첫 단계로부터
     * {@code windowDays} 안에 끝나야 한다. 2026-08 실데이터로 Mixpanel 화면 숫자와 일치를 확인했다.
     *
     * <p>{@code groupBy} 를 주면 사용자 대신 그 속성 값(예: {@code class_id})으로 묶어 센다.
     * 운영자가 만들고 참가자가 들어오는 클래스 퍼널처럼 단계마다 사람이 다른 흐름을 한 줄로 본다 (#687).
     * 속성이 없는 이벤트는 그 퍼널에서 빠진다.
     *
     * <p>단계마다 앞 단계에서 넘어오기까지 걸린 시간의 중앙값(초)을 함께 낸다 (#687 체류시간).
     */
    public MixpanelFunnelReport fetchFunnel(
            List<String> steps, int windowDays, LocalDate from, LocalDate to, String groupBy) {
        String group = StringUtils.hasText(groupBy) ? groupBy.trim() : null;
        if (!isConfigured()) {
            return MixpanelFunnelReport.empty(AnalyticsStatus.NOT_CONFIGURED, steps, windowDays, from, to, group);
        }

        AnalyticsHttpResponse response = transport.get(uri(from, to),
                Map.of("Authorization", "Basic " + basicCredentials()));
        if (!response.isSuccess()) {
            log.warn("[Mixpanel] 퍼널용 원본 이벤트 조회 실패: status={}", response.statusCode());
            return MixpanelFunnelReport.empty(AnalyticsStatus.UNAVAILABLE, steps, windowDays, from, to, group);
        }

        try {
            FunnelWalk walk = walkFunnel(response.body(), steps, windowDays, from, to, group);
            return funnelReport(steps, windowDays, from, to, group, walk);
        } catch (Exception e) {
            log.warn("[Mixpanel] 퍼널 계산 실패: {}", e.getClass().getSimpleName());
            return MixpanelFunnelReport.empty(AnalyticsStatus.UNAVAILABLE, steps, windowDays, from, to, group);
        }
    }

    /**
     * 단계 하나를 가리키는 조건.
     * <ul>
     *   <li>{@code battle_step} : 이벤트 이름만</li>
     *   <li>{@code battle_step:step_name=pre_vote} : 속성 값이 같음</li>
     *   <li>{@code network_request:method=POST;path~/api/v1/classes/\d+/battle/votes/post} :
     *       조건 여러 개는 {@code ;} 로 잇고, {@code ~} 는 정규식 전체 일치 (#687)</li>
     * </ul>
     *
     * <p>배틀 흐름은 선택·재생·투표가 모두 {@code battle_step} 한 이벤트로 들어오고
     * {@code step_name} 으로만 갈린다. 속성을 못 좁히면 이 구간이 한 단계로 뭉쳐 이탈율을 볼 수 없다.
     */
    record StepMatcher(String spec, String event, List<Condition> conditions) {

        record Condition(String property, String value, java.util.regex.Pattern pattern) {
            boolean matches(JsonNode properties) {
                JsonNode actual = properties.get(property);
                if (actual == null || actual.isNull() || actual.isContainerNode()) {
                    return false;
                }
                String text = actual.asText();
                return pattern != null ? pattern.matcher(text).matches() : value.equals(text);
            }
        }

        static StepMatcher parse(String spec) {
            int marker = spec.indexOf(':');
            if (marker < 0) {
                return new StepMatcher(spec, spec, List.of());
            }
            String event = spec.substring(0, marker);
            List<Condition> conditions = new ArrayList<>();
            for (String filter : spec.substring(marker + 1).split(";")) {
                if (filter.isBlank()) {
                    continue;
                }
                int equals = filter.indexOf('=');
                int tilde = filter.indexOf('~');
                boolean regex = tilde >= 0 && (equals < 0 || tilde < equals);
                int operator = regex ? tilde : equals;
                if (operator <= 0) {
                    throw new IllegalArgumentException(
                            "퍼널 단계 속성은 property=value 또는 property~정규식 형태여야 합니다: " + spec);
                }
                String property = filter.substring(0, operator).trim();
                String value = filter.substring(operator + 1);
                try {
                    conditions.add(new Condition(property, value,
                            regex ? java.util.regex.Pattern.compile(value) : null));
                } catch (java.util.regex.PatternSyntaxException e) {
                    throw new IllegalArgumentException("퍼널 단계 정규식이 올바르지 않습니다: " + spec);
                }
            }
            return new StepMatcher(spec, event, List.copyOf(conditions));
        }

        boolean matches(String event, JsonNode properties) {
            if (!this.event.equals(event)) {
                return false;
            }
            for (Condition condition : conditions) {
                if (!condition.matches(properties)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** 단계별 도달 수와, 앞 단계에서 넘어오기까지 걸린 초(대상별 1건) */
    private record FunnelWalk(long[] reached, List<List<Long>> secondsFromPrevious, List<Long> secondsToComplete) {
    }

    /** 대상(사용자 또는 groupBy 값)별로 단계 이벤트만 모아 시간순으로 훑는다. 단계에 없는 이벤트는 담지 않아 메모리를 아낀다. */
    private FunnelWalk walkFunnel(
            String body, List<String> steps, int windowDays, LocalDate from, LocalDate to, String groupBy)
            throws Exception {
        List<StepMatcher> matchers = steps.stream().map(StepMatcher::parse).toList();
        Map<String, List<long[]>> byTarget = new HashMap<>();
        Set<String> seen = new HashSet<>();

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
            if (isDuplicate(seen, event, properties)) {
                continue;
            }
            Instant occurredAt = Instant.ofEpochSecond(time.asLong());
            LocalDate date = occurredAt.atZone(projectZone).toLocalDate();
            if (date.isBefore(from) || date.isAfter(to)) {
                continue;
            }
            String target = text(properties.get(groupBy == null ? "distinct_id" : groupBy));
            if (target == null) {
                // 대상을 못 가리는 이벤트는 고유 전환을 셀 수 없다.
                continue;
            }
            // 이벤트 하나는 처음 맞는 단계 하나에만 쓴다 (같은 이벤트가 두 단계를 동시에 채우지 않게, 기존 규칙 유지)
            for (int index = 0; index < matchers.size(); index++) {
                if (matchers.get(index).matches(event, properties)) {
                    byTarget.computeIfAbsent(target, key -> new ArrayList<>())
                            .add(new long[]{time.asLong(), index});
                    break;
                }
            }
        }

        long[] reached = new long[steps.size()];
        List<List<Long>> fromPrevious = new ArrayList<>();
        for (int index = 0; index < steps.size(); index++) {
            fromPrevious.add(new ArrayList<>());
        }
        List<Long> toComplete = new ArrayList<>();
        long window = (long) windowDays * 86_400L;
        for (List<long[]> events : byTarget.values()) {
            events.sort(Comparator.comparingLong(entry -> entry[0]));
            Long entry = firstTimeOf(events, 0);
            if (entry == null) {
                continue;
            }
            reached[0]++;
            long cursor = entry;
            long deadline = entry + window;
            boolean completed = true;
            for (int step = 1; step < steps.size(); step++) {
                Long next = nextTimeOf(events, step, cursor, deadline);
                if (next == null) {
                    completed = false;
                    break;
                }
                reached[step]++;
                fromPrevious.get(step).add(next - cursor);
                cursor = next;
            }
            if (completed) {
                toComplete.add(cursor - entry);
            }
        }
        return new FunnelWalk(reached, fromPrevious, toComplete);
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
            List<String> steps, int windowDays, LocalDate from, LocalDate to, String groupBy, FunnelWalk walk) {
        long[] reached = walk.reached();
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
                    previous == null ? null : percentage(previous - users, previous),
                    index == 0 ? null : median(walk.secondsFromPrevious().get(index))));
        }
        long completed = reached[reached.length - 1];
        return new MixpanelFunnelReport(AnalyticsStatus.CONNECTED, Instant.now(), from, to, windowDays,
                entered, completed, percentage(completed, entered), groupBy, median(walk.secondsToComplete()), rows);
    }

    /** 값이 없으면 null. 짝수 개면 가운데 두 값의 평균(내림) */
    static Long median(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(null);
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle) : (sorted.get(middle - 1) + sorted.get(middle)) / 2;
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

    private boolean usesServiceAccount() {
        return StringUtils.hasText(serviceAccountUsername) && StringUtils.hasText(serviceAccountSecret)
                && StringUtils.hasText(projectId);
    }

    private boolean isConfigured() {
        return usesServiceAccount() || StringUtils.hasText(apiSecret);
    }

    private String basicCredentials() {
        // 서비스 계정은 사용자명:비밀, 레거시 방식은 API 비밀을 사용자명 자리에 두고 비밀번호를 비운다.
        String credentials = usesServiceAccount()
                ? serviceAccountUsername + ":" + serviceAccountSecret
                : apiSecret + ":";
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Export 는 UTC 날짜로 잘리므로 projectZone 날짜 [from, to] 를 덮도록 앞뒤 하루씩 넓혀 받는다.
     * 오늘(UTC) 이후 날짜를 넣으면 Mixpanel 이 거절하므로 끝은 오늘로 자른다.
     */
    private URI uri(LocalDate from, LocalDate to) {
        LocalDate todayUtc = LocalDate.now(java.time.ZoneOffset.UTC);
        LocalDate exportTo = to.plusDays(1).isAfter(todayUtc) ? todayUtc : to.plusDays(1);
        LocalDate exportFrom = from.minusDays(1);
        if (exportTo.isBefore(exportFrom)) {
            exportTo = exportFrom;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/2.0/export")
                .queryParam("from_date", exportFrom)
                .queryParam("to_date", exportTo);
        if (usesServiceAccount()) {
            builder.queryParam("project_id", projectId);
        }
        return builder.build().toUri();
    }
}
