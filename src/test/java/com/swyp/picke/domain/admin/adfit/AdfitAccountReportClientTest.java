package com.swyp.picke.domain.admin.adfit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AdfitAccountReportClientTest {
    private final LocalDate from = LocalDate.of(2026, 9, 8);
    private final LocalDate to = LocalDate.of(2026, 9, 10);

    @Test void springCreatesClientWithoutConfiguredCredentials() {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.registerBean(AdfitHttpTransport.class,
                    () -> new CapturingTransport(response(200, "application/json", "[]")));
            context.register(AdfitAccountReportClient.class);
            context.refresh();
            assertThat(context.getBean(AdfitAccountReportClient.class)).isNotNull();
        }
    }

    @Test void returnsNotConfiguredWithoutCookie() {
        var client = client("", response(200, "application/json", "[]"));
        var result = client.fetch(from, to, 3);
        assertThat(result.status()).isEqualTo(AdfitAccountReportStatus.NOT_CONFIGURED);
        assertThat(result.days()).hasSize(3);
        assertThat(result.days()).extracting(AdfitReport.AccountDay::revenue).containsOnlyNulls();
    }

    @Test void fetchesConsoleDailyRevenueWithIsoDateParameters() {
        var transport = new CapturingTransport(response(200, "application/json", """
                [
                  {"reportDate":"2026-09-10","profit":null,"ctr":1.5,"ecpm":2,"fillRate":3,"winFillRate":4},
                  {"reportDate":"2026-09-09","profit":5},
                  {"reportDate":"2026-09-08","profit":0}
                ]
                """));
        var client = new AdfitAccountReportClient("KAKAO=secret", transport, new ObjectMapper());
        var result = client.fetch(from, to, 3);
        assertThat(transport.uri.toString()).contains("dayType=DAY", "startDate=2026-09-08", "endDate=2026-09-10");
        assertThat(result.status()).isEqualTo(AdfitAccountReportStatus.CONNECTED);
        assertThat(result.fetchedAt()).isNotNull();
        assertThat(result.reportedDays()).isEqualTo(2);
        assertThat(result.revenue()).isEqualByComparingTo("5");
        assertThat(result.roi()).isNull();
        assertThat(result.days()).extracting(AdfitReport.AccountDay::date)
                .containsExactly(to, LocalDate.of(2026, 9, 9), from);
        assertThat(result.days().getFirst().revenue()).isNull();
        assertThat(result.days().getFirst().ctr()).isEqualByComparingTo("1.5");
        assertThat(result.days().get(1).revenue()).isEqualByComparingTo("5");
        assertThat(result.days().get(2).revenue()).isEqualByComparingTo("0");
    }

    @Test void treatsAuthFailuresRedirectsAndHtmlAsReconnectRequired() {
        assertReconnectRequired(response(401, "application/json", "{}"));
        assertReconnectRequired(response(419, "application/json", "{}"));
        assertReconnectRequired(response(302, "text/plain", ""));
        assertReconnectRequired(response(200, "text/html", "<html>login</html>"));
    }

    @Test void rejectsUnavailableAndInvalidSchemaWithoutFabricatingRevenue() {
        assertUnavailable(response(500, "application/json", "{}"));
        assertUnavailable(response(200, "text/plain", "[]"));
        assertUnavailable(response(200, "application/json", "{}"));
        assertUnavailable(response(200, "application/json", "[{\"reportDate\":\"2026-09-10\"}]"));
        assertUnavailable(response(200, "application/json", "[{\"reportDate\":\"2026-09-10\",\"profit\":\"5\"}]"));
        assertUnavailable(response(200, "application/json", "[{\"reportDate\":\"2026-09-11\",\"profit\":5}]"));
        assertUnavailable(response(200, "application/json", """
                [
                  {"reportDate":"2026-09-10","profit":5},
                  {"reportDate":"2026-09-10","profit":6}
                ]
                """));
    }

    private void assertReconnectRequired(AdfitHttpResponse response) {
        var result = client("KAKAO=secret", response).fetch(from, to, 3);
        assertThat(result.status()).isEqualTo(AdfitAccountReportStatus.RECONNECT_REQUIRED);
        assertThat(result.revenue()).isNull();
    }

    private void assertUnavailable(AdfitHttpResponse response) {
        var result = client("KAKAO=secret", response).fetch(from, to, 3);
        assertThat(result.status()).isEqualTo(AdfitAccountReportStatus.UNAVAILABLE);
        assertThat(result.revenue()).isNull();
        assertThat(result.days()).extracting(AdfitReport.AccountDay::revenue).containsOnlyNulls();
    }

    private AdfitAccountReportClient client(String cookie, AdfitHttpResponse response) {
        return new AdfitAccountReportClient(cookie, new CapturingTransport(response), new ObjectMapper());
    }

    private AdfitHttpResponse response(int status, String contentType, String body) {
        return new AdfitHttpResponse(status, Map.of("content-type", List.of(contentType)), body);
    }

    private static class CapturingTransport implements AdfitHttpTransport {
        private final AdfitHttpResponse response;
        private URI uri;

        CapturingTransport(AdfitHttpResponse response) {
            this.response = response;
        }

        @Override
        public AdfitHttpResponse get(URI uri, String sessionCookie) {
            this.uri = uri;
            return response;
        }
    }
}
