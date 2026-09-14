package com.swyp.picke.domain.admin.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Mixpanel 이벤트 일별 발생 수.
 *
 * @param events 요청한 이벤트별 시리즈. 이벤트 이름은 서버가 알 수 없어 설정 또는 요청 파라미터로 받는다.
 */
public record MixpanelEventReport(
        AnalyticsStatus status,
        Instant fetchedAt,
        LocalDate from,
        LocalDate to,
        List<EventSeries> events) {

    public record EventSeries(String event, Long total, List<Day> days) {
    }

    /** @param count 해당 날짜 발생 수. Mixpanel 이 값을 주지 않은 날짜는 null 이며 0 으로 대체하지 않는다. */
    public record Day(LocalDate date, Long count) {
    }

    static MixpanelEventReport empty(AnalyticsStatus status, LocalDate from, LocalDate to) {
        return new MixpanelEventReport(status, null, from, to, List.of());
    }
}
