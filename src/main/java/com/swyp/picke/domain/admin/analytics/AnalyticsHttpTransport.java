package com.swyp.picke.domain.admin.analytics;

import java.net.URI;
import java.util.Map;

interface AnalyticsHttpTransport {
    AnalyticsHttpResponse get(URI uri, Map<String, String> headers);
}
