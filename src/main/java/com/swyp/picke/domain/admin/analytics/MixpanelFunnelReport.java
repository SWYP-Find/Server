package com.swyp.picke.domain.admin.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 지정한 이벤트 순서를 사용자가 끝까지 밟았는지 센 퍼널. 원본 이벤트로 서버가 직접 계산한다.
 *
 * <p>고유 사용자 기준이다. 한 사람이 같은 단계를 여러 번 밟아도 한 번으로 센다.
 * 다음 단계는 앞 단계 이후에 일어나야 하고, 첫 단계로부터 {@code windowDays} 안에 끝나야 한다.
 * Mixpanel 화면의 Unique Conversion·전환 윈도우와 같은 규칙이다.
 *
 * @param enteredUsers   첫 단계를 밟은 사용자 수.
 * @param completedUsers 마지막 단계까지 밟은 사용자 수.
 * @param conversionRate 첫 단계 대비 마지막 단계 전환율(%). 첫 단계가 0명이면 null.
 */
public record MixpanelFunnelReport(
        AnalyticsStatus status,
        Instant fetchedAt,
        LocalDate from,
        LocalDate to,
        int windowDays,
        Long enteredUsers,
        Long completedUsers,
        Double conversionRate,
        List<Step> steps) {

    /**
     * @param order                 1부터 시작하는 단계 순서.
     * @param users                 이 단계까지 밟은 사용자 수.
     * @param conversionFromEntry   첫 단계 대비 전환율(%).
     * @param conversionFromPrevious 앞 단계 대비 전환율(%). 첫 단계는 null.
     * @param droppedFromPrevious   앞 단계에서 이탈한 사용자 수. 첫 단계는 null.
     * @param dropOffFromPrevious   앞 단계 대비 이탈율(%). 첫 단계는 null.
     */
    public record Step(
            int order,
            String event,
            Long users,
            Double conversionFromEntry,
            Double conversionFromPrevious,
            Long droppedFromPrevious,
            Double dropOffFromPrevious) {
    }

    static MixpanelFunnelReport empty(
            AnalyticsStatus status, List<String> steps, int windowDays, LocalDate from, LocalDate to) {
        List<Step> emptySteps = java.util.stream.IntStream.range(0, steps.size())
                .mapToObj(index -> new Step(index + 1, steps.get(index), null, null, null, null, null))
                .toList();
        return new MixpanelFunnelReport(status, null, from, to, windowDays, null, null, null, emptySteps);
    }
}
