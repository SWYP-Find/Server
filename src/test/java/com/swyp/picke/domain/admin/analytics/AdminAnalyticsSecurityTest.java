package com.swyp.picke.domain.admin.analytics;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(AdminAnalyticsSecurityTest.Config.class)
class AdminAnalyticsSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean MixpanelClient mixpanelClient() { return mock(MixpanelClient.class); }
        @Bean SentryClient sentryClient() { return mock(SentryClient.class); }
        @Bean AdminAnalyticsController controller(MixpanelClient mixpanel, SentryClient sentry) {
            return new AdminAnalyticsController(mixpanel, sentry);
        }
    }

    @Autowired AdminAnalyticsController controller;

    private final LocalDate date = LocalDate.of(2026, 9, 1);

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("일반 사용자는 지표를 볼 수 없다")
    void regularUserCannotReadMetrics() {
        assertThatThrownBy(() -> controller.mixpanel(date, date, List.of("앱 실행")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.sentry(date, date)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("관리자는 지표를 본다")
    void adminCanReadMetrics() {
        assertThatCode(() -> controller.mixpanel(date, date, List.of("앱 실행"))).doesNotThrowAnyException();
        assertThatCode(() -> controller.sentry(date, date)).doesNotThrowAnyException();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("366일을 넘는 기간은 거절한다")
    void rejectsTooWideRange() {
        assertThatThrownBy(() -> controller.sentry(date, date.plusYears(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
