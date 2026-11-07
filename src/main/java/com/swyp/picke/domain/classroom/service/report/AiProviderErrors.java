package com.swyp.picke.domain.classroom.service.report;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/** 클래스 AI 호출의 제공자 오류 분류. 재시도 여부와 ai_jobs.error_code 를 정한다 (PRD §17 "연결 오류·429·5xx 만 재시도") */
public final class AiProviderErrors {

    private AiProviderErrors() {
    }

    /** 크레딧 소진(insufficient_quota)도 429 로 오지만 다시 호출해도 소용없으므로 재시도하지 않는다 */
    public static boolean retryable(Exception e) {
        if (e instanceof HttpClientErrorException.TooManyRequests tooMany) {
            return !isQuotaExhausted(tooMany);
        }
        return e instanceof ResourceAccessException || e instanceof HttpServerErrorException;
    }

    public static String errorCodeOf(Exception e) {
        if (e instanceof HttpClientErrorException.TooManyRequests tooMany && isQuotaExhausted(tooMany)) {
            return "INSUFFICIENT_QUOTA";
        }
        String name = e.getClass().getSimpleName();
        if (name.contains("Timeout") || (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("Timeout"))) {
            return "TIMEOUT";
        }
        return name.length() > 50 ? name.substring(0, 50) : name;
    }

    private static boolean isQuotaExhausted(HttpClientErrorException e) {
        return e.getResponseBodyAsString().contains("insufficient_quota");
    }
}
