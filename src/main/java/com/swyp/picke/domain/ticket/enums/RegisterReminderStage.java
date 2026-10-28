package com.swyp.picke.domain.ticket.enums;

import java.time.LocalDateTime;

/**
 * 미등록 이용권 등록기한 임박 알림 단계 (PRD "등록기한 30일·7일 전 알림").
 * 단계마다 등록기한이 (now + 다음 단계 일수, now + 이 단계 일수] 안에 있는 이용권이 대상이다.
 * 30일 알림을 놓친 채 7일 안으로 들어온 이용권은 7일 알림만 대상이 된다.
 */
public enum RegisterReminderStage {
    D30(30, 7),
    D7(7, 0);

    private final int daysBefore;
    private final int nextStageDaysBefore;

    RegisterReminderStage(int daysBefore, int nextStageDaysBefore) {
        this.daysBefore = daysBefore;
        this.nextStageDaysBefore = nextStageDaysBefore;
    }

    /** 대상 등록기한 구간 시작(제외) */
    public LocalDateTime windowStart(LocalDateTime now) {
        return now.plusDays(nextStageDaysBefore);
    }

    /** 대상 등록기한 구간 끝(포함) */
    public LocalDateTime windowEnd(LocalDateTime now) {
        return now.plusDays(daysBefore);
    }
}
