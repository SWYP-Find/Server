package com.swyp.picke.domain.admin.analytics;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class JavaNetAnalyticsHttpTransport implements AnalyticsHttpTransport {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    /** Mixpanel 원본 이벤트는 한 달치가 수십 MB 가 된다. 내려받는 시간을 감당할 만큼 둔다. */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public AnalyticsHttpResponse get(URI uri, Map<String, String> headers) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json");
            headers.forEach(request::header);
            HttpResponse<String> response = client.send(request.GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            return new AnalyticsHttpResponse(response.statusCode(), response.headers().map(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new AnalyticsHttpResponse(0, Map.of("x-analytics-error", List.of("interrupted")), "");
        } catch (Exception e) {
            // 관리자 지표 조회다. 외부 장애가 관리자 화면 전체를 500 으로 만들지 않게 한다.
            return new AnalyticsHttpResponse(0, Map.of("x-analytics-error", List.of("request_failed")), "");
        }
    }
}
