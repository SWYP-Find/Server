package com.swyp.picke.domain.admin.analytics;

import java.util.List;
import java.util.Map;

record AnalyticsHttpResponse(int statusCode, Map<String, List<String>> headers, String body) {
    boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    boolean isJson() {
        return headers.entrySet().stream()
                .filter(entry -> "content-type".equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(value -> value.toLowerCase().contains("application/json"));
    }
}
