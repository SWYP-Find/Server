package com.swyp.picke.domain.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationScheduleTest {

    @Test
    @DisplayName("활성화 상태이고 발송 시각(분 단위)이 같고 오늘 아직 발송되지 않았다면 발송 대상이다")
    void isDue_true_whenEnabledAndTimeMatchesAndNotSentToday() {
        NotificationSchedule schedule = NotificationSchedule.builder()
                .title("오늘의 질문")
                .subtitle("지금 확인해보세요")
                .sendTime(LocalTime.of(19, 0))
                .enabled(true)
                .build();

        boolean due = schedule.isDue(LocalTime.of(19, 0, 37), LocalDate.of(2026, 8, 8));

        assertThat(due).isTrue();
    }

    @Test
    @DisplayName("비활성화 상태면 시각이 같아도 발송 대상이 아니다")
    void isDue_false_whenDisabled() {
        NotificationSchedule schedule = NotificationSchedule.builder()
                .title("오늘의 질문")
                .subtitle("지금 확인해보세요")
                .sendTime(LocalTime.of(19, 0))
                .enabled(false)
                .build();

        boolean due = schedule.isDue(LocalTime.of(19, 0), LocalDate.of(2026, 8, 8));

        assertThat(due).isFalse();
    }

    @Test
    @DisplayName("발송 시각(분 단위)이 다르면 발송 대상이 아니다")
    void isDue_false_whenTimeDoesNotMatch() {
        NotificationSchedule schedule = NotificationSchedule.builder()
                .title("오늘의 질문")
                .subtitle("지금 확인해보세요")
                .sendTime(LocalTime.of(19, 0))
                .enabled(true)
                .build();

        boolean due = schedule.isDue(LocalTime.of(19, 1), LocalDate.of(2026, 8, 8));

        assertThat(due).isFalse();
    }

    @Test
    @DisplayName("같은 날 이미 발송했다면 다시 발송 대상이 되지 않는다")
    void isDue_false_whenAlreadySentToday() {
        NotificationSchedule schedule = NotificationSchedule.builder()
                .title("오늘의 질문")
                .subtitle("지금 확인해보세요")
                .sendTime(LocalTime.of(19, 0))
                .enabled(true)
                .build();
        LocalDate today = LocalDate.of(2026, 8, 8);
        schedule.markSent(today);

        boolean due = schedule.isDue(LocalTime.of(19, 0), today);

        assertThat(due).isFalse();
    }

    @Test
    @DisplayName("이전에 발송했더라도 날짜가 바뀌면 다시 발송 대상이 된다")
    void isDue_true_whenSentOnDifferentDay() {
        NotificationSchedule schedule = NotificationSchedule.builder()
                .title("오늘의 질문")
                .subtitle("지금 확인해보세요")
                .sendTime(LocalTime.of(19, 0))
                .enabled(true)
                .build();
        schedule.markSent(LocalDate.of(2026, 8, 7));

        boolean due = schedule.isDue(LocalTime.of(19, 0), LocalDate.of(2026, 8, 8));

        assertThat(due).isTrue();
    }
}
