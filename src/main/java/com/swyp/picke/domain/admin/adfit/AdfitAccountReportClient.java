package com.swyp.picke.domain.admin.adfit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class AdfitAccountReportClient {
    private static final String REPORT_URL =
            "https://adfit.kakao.com/api/v2/report/accountTotal/periodicIndicators";

    /** 쿠키는 조회할 때마다 다시 읽는다. 관리자가 만료된 값을 갈아끼우면 재배포 없이 바로 반영돼야 한다. */
    private final Supplier<String> sessionCookieSupplier;
    private final AdfitHttpTransport transport;
    private final ObjectMapper objectMapper;

    @Autowired
    public AdfitAccountReportClient(AdfitSessionCookieStore cookieStore, AdfitHttpTransport transport) {
        this(cookieStore::current, transport, new ObjectMapper());
    }

    AdfitAccountReportClient(Supplier<String> sessionCookieSupplier, AdfitHttpTransport transport,
                             ObjectMapper objectMapper) {
        this.sessionCookieSupplier = sessionCookieSupplier;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    public AdfitReport.AccountReport fetch(LocalDate from, LocalDate to, long expectedDays) {
        validateRange(from, to);
        List<AdfitReport.AccountDay> emptyDays = nullDays(from, to);
        String sessionCookie = sessionCookieSupplier.get();
        if (!StringUtils.hasText(sessionCookie)) {
            return report(AdfitAccountReportStatus.NOT_CONFIGURED, null, expectedDays, null, emptyDays);
        }

        AdfitHttpResponse response = transport.get(uri(from, to), sessionCookie);
        if (response.statusCode() == 401 || response.statusCode() == 403 || response.statusCode() == 419
                || response.isRedirect()
                || looksLikeHtml(response.body())) {
            return report(AdfitAccountReportStatus.RECONNECT_REQUIRED, null, expectedDays, null, emptyDays);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300 || !response.isJson()) {
            return report(AdfitAccountReportStatus.UNAVAILABLE, null, expectedDays, null, emptyDays);
        }

        try {
            List<AdfitReport.AccountDay> days = parseDays(response.body(), from, to);
            BigDecimal revenue = days.stream()
                    .map(AdfitReport.AccountDay::revenue)
                    .filter(value -> value != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long reportedDays = days.stream().filter(day -> day.revenue() != null).count();
            return new AdfitReport.AccountReport(AdfitAccountReportStatus.CONNECTED, Instant.now(),
                    reportedDays, expectedDays, reportedDays == 0 ? null : revenue, null, null, days);
        } catch (Exception e) {
            log.warn("[AdFit] 계정 수익 보고서 응답 파싱 실패: {}", e.getClass().getSimpleName());
            return report(AdfitAccountReportStatus.UNAVAILABLE, null, expectedDays, null, emptyDays);
        }
    }

    private URI uri(LocalDate from, LocalDate to) {
        return UriComponentsBuilder.fromUriString(REPORT_URL)
                .queryParam("dayType", "DAY")
                .queryParam("startDate", from)
                .queryParam("endDate", to)
                .build()
                .toUri();
    }

    private List<AdfitReport.AccountDay> parseDays(String body, LocalDate from, LocalDate to) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (!root.isArray()) {
            throw new IllegalArgumentException("AdFit report root must be an array.");
        }

        Map<LocalDate, AdfitReport.AccountDay> byDate = new HashMap<>();
        for (JsonNode node : root) {
            LocalDate date = LocalDate.parse(requiredText(node, "reportDate"));
            if (date.isBefore(from) || date.isAfter(to)) {
                throw new IllegalArgumentException("AdFit report date is out of range.");
            }
            if (!node.has("profit")) {
                throw new IllegalArgumentException("AdFit report profit is missing.");
            }
            AdfitReport.AccountDay day = new AdfitReport.AccountDay(date, decimalOrNull(node, "profit"),
                    decimalOrNull(node, "ctr"), decimalOrNull(node, "ecpm"),
                    decimalOrNull(node, "fillRate"), decimalOrNull(node, "winFillRate"));
            if (byDate.putIfAbsent(date, day) != null) {
                throw new IllegalArgumentException("AdFit report date is duplicated.");
            }
        }

        List<AdfitReport.AccountDay> days = new ArrayList<>();
        LocalDate cursor = to;
        while (!cursor.isBefore(from)) {
            days.add(byDate.getOrDefault(cursor,
                    new AdfitReport.AccountDay(cursor, null, null, null, null, null)));
            cursor = cursor.minusDays(1);
        }
        return days;
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual()) {
            throw new IllegalArgumentException("AdFit report " + field + " is invalid.");
        }
        return value.asText();
    }

    private BigDecimal decimalOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isNumber()) {
            throw new IllegalArgumentException("AdFit report " + field + " is invalid.");
        }
        return value.decimalValue();
    }

    private List<AdfitReport.AccountDay> nullDays(LocalDate from, LocalDate to) {
        List<AdfitReport.AccountDay> days = new ArrayList<>();
        LocalDate cursor = to;
        while (!cursor.isBefore(from)) {
            days.add(new AdfitReport.AccountDay(cursor, null, null, null, null, null));
            cursor = cursor.minusDays(1);
        }
        return days;
    }

    private AdfitReport.AccountReport report(AdfitAccountReportStatus status, Instant fetchedAt,
                                             long expectedDays, BigDecimal revenue,
                                             List<AdfitReport.AccountDay> days) {
        return new AdfitReport.AccountReport(status, fetchedAt, 0, expectedDays, revenue, null, null, days);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        long expected = ChronoUnit.DAYS.between(from, to) + 1;
        if (expected < 1 || expected > 366) {
            throw new IllegalArgumentException("조회 기간은 1일부터 366일까지입니다.");
        }
    }

    private boolean looksLikeHtml(String body) {
        return body != null && body.stripLeading().startsWith("<");
    }
}
