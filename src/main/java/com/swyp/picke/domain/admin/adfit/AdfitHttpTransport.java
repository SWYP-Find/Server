package com.swyp.picke.domain.admin.adfit;

import java.net.URI;

interface AdfitHttpTransport {
    AdfitHttpResponse get(URI uri, String sessionCookie);
}
