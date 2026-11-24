package com.swyp.picke.global.infra.mixpanel;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Mixpanel Import API ({@code POST /import?strict=1}) 로 보낸다 (#689).
 *
 * <p>인증은 조회({@code MixpanelClient})와 같다. 서비스 계정(사용자명·비밀·프로젝트 ID)이 있으면 우선하고
 * {@code project_id} 를 붙이며, 없으면 프로젝트 API 비밀을 Basic 사용자명 자리에 넣는다.
 */
@Slf4j
@Component
public class HttpMixpanelImportSender implements MixpanelImportSender {

    private final String baseUrl;
    private final String apiSecret;
    private final String serviceAccountUsername;
    private final String serviceAccountSecret;
    private final String projectId;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public HttpMixpanelImportSender(
            @Value("${picke.analytics.mixpanel.import-base-url:https://api.mixpanel.com}") String baseUrl,
            @Value("${picke.analytics.mixpanel.api-secret:${MIXPANEL_API_SECRET:}}") String apiSecret,
            @Value("${picke.analytics.mixpanel.service-account-username:${MIXPANEL_SERVICE_ACCOUNT_USERNAME:}}")
            String serviceAccountUsername,
            @Value("${picke.analytics.mixpanel.service-account-secret:${MIXPANEL_SERVICE_ACCOUNT_SECRET:}}")
            String serviceAccountSecret,
            @Value("${picke.analytics.mixpanel.project-id:${MIXPANEL_PROJECT_ID:}}") String projectId) {
        this.baseUrl = baseUrl;
        this.apiSecret = apiSecret;
        this.serviceAccountUsername = serviceAccountUsername;
        this.serviceAccountSecret = serviceAccountSecret;
        this.projectId = projectId;
    }

    @Override
    public boolean send(List<Map<String, Object>> events) {
        boolean serviceAccount = StringUtils.hasText(serviceAccountUsername) && StringUtils.hasText(serviceAccountSecret)
                && StringUtils.hasText(projectId);
        if (!serviceAccount && !StringUtils.hasText(apiSecret)) {
            log.debug("[Mixpanel] 전송 설정 없음 - 건너뜀");
            return false;
        }
        try {
            String credentials = serviceAccount ? serviceAccountUsername + ":" + serviceAccountSecret : apiSecret + ":";
            String uri = baseUrl + "/import?strict=1" + (serviceAccount ? "&project_id=" + projectId : "");
            HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + Base64.getEncoder()
                            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(events)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                // 본문에는 실패 사유만 있고 사용자 정보는 없다. 길이를 잘라 남긴다
                String body = response.body() == null ? "" : response.body();
                log.warn("[Mixpanel] 이벤트 전송 실패 status={} body={}", response.statusCode(),
                        body.length() > 300 ? body.substring(0, 300) : body);
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Mixpanel] 이벤트 전송 중단");
            return false;
        } catch (Exception e) {
            log.warn("[Mixpanel] 이벤트 전송 오류: {}", e.getClass().getSimpleName());
            return false;
        }
    }
}
