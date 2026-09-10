package com.swyp.picke.domain.admin.adfit;

import java.util.List;
import java.util.Map;

record AdfitHttpResponse(int statusCode, Map<String, List<String>> headers, String body) {
    boolean isRedirect() {
        return statusCode >= 300 && statusCode < 400;
    }

    boolean isJson() {
        return headers.entrySet().stream()
                .filter(entry -> "content-type".equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(value -> value.toLowerCase().contains("application/json"));
    }
}
