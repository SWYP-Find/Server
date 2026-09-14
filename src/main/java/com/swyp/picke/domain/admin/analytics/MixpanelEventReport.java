package com.swyp.picke.domain.admin.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Mixpanel 이벤트 일별 발생 수. 원본 이벤트를 내려받아 서버가 센 값이다.
 *
 * @param availableEvents 조회 기간 원본에 나타난 전체 이벤트 이름.
 * @param events           발생 수 내림차순. 조회할 이벤트를 지정하지 않으면 기간에 나타난 이벤트 전부다.
 */
public record MixpanelEventReport(
        AnalyticsStatus status,
        Instant fetchedAt,
        LocalDate from,
        LocalDate to,
        Long totalEvents,
        Long uniqueUsers,
        LocalDate summaryDate,
        Long activeUsers,
        Long signUps,
        List<SignUpDay> signUpDays,
        List<String> availableEvents,
        List<EventSeries> events) {

    public record EventSeries(
            String event,
            Long total,
            Long uniqueUsers,
            Instant firstSeen,
            Instant lastSeen,
            List<Day> days) {
    }

    /**
     * @param count 해당 날짜 발생 수. 원본을 전부 받아 세므로 이벤트가 없던 날짜는 미집계가 아니라 0 이다.
     */
    public record Day(LocalDate date, Long count, Long uniqueUsers) {
    }

    public record SignUpDay(LocalDate date, Long count) {
    }

    static MixpanelEventReport empty(AnalyticsStatus status, LocalDate from, LocalDate to) {
        return new MixpanelEventReport(
                status, null, from, to, null, null, to, null, null, List.of(), List.of(), List.of());
    }
}
