package com.swyp.picke.domain.admin.adfit;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(AdminAdfitSecurityTest.Config.class)
class AdminAdfitSecurityTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean AdfitReportService service() { return mock(AdfitReportService.class); }
        @Bean AdminAdfitController controller(AdfitReportService service) { return new AdminAdfitController(service); }
    }
    @Autowired AdminAdfitController controller;

    @Test @WithMockUser(roles = "USER")
    void regularUserCannotReadOrWriteIncome() {
        var date = LocalDate.of(2026, 9, 1);
        assertThatThrownBy(() -> controller.report(date, date)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.save(new AdfitDailyRequest(date, AdfitUnit.BANNER,
                BigDecimal.ONE, BigDecimal.ONE, AdfitCostBasis.AD_OPERATIONS)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void adminCanReadAndWriteIncome() {
        var date = LocalDate.of(2026, 9, 1);
        assertThatCode(() -> controller.report(date, date)).doesNotThrowAnyException();
        assertThatCode(() -> controller.save(new AdfitDailyRequest(date, AdfitUnit.BANNER,
                BigDecimal.ONE, BigDecimal.ONE, AdfitCostBasis.AD_OPERATIONS))).doesNotThrowAnyException();
    }

    @Test void rejectsNegativeMoneyAndMissingBasis() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var errors = factory.getValidator().validate(new AdfitDailyRequest(LocalDate.now(),
                    AdfitUnit.BANNER, new BigDecimal("-1"), new BigDecimal("1.001"), null));
            assertThat(errors).extracting(error -> error.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("revenue", "cost", "costBasis");
        }
    }
}
