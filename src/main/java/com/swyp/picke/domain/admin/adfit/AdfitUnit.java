package com.swyp.picke.domain.admin.adfit;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdfitUnit {
    NATIVE_WIDE("네이티브 광고", List.of("홈", "큐레이션", "마이페이지"), "네이티브 2:1"),
    BANNER("탐색 배너", List.of("탐색"), "배너 320×100"),
    APP_TRANSITION("앱 시작 팝업", List.of("앱 시작"), "전면 팝업");

    private final String displayName;
    private final List<String> placements;
    private final String format;
}
