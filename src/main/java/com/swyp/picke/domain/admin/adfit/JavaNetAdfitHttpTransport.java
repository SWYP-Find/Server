package com.swyp.picke.domain.admin.adfit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class JavaNetAdfitHttpTransport implements AdfitHttpTransport {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public AdfitHttpResponse get(URI uri, String sessionCookie) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Referer", "https://adfit.kakao.com/report")
                    .header("Cookie", sessionCookie)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new AdfitHttpResponse(response.statusCode(), response.headers().map(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new AdfitHttpResponse(0, Map.of("x-adfit-error", List.of("interrupted")), "");
        } catch (Exception e) {
            return new AdfitHttpResponse(0, Map.of("x-adfit-error", List.of("request_failed")), "");
        }
    }
}
