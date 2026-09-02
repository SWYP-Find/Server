package com.swyp.picke.domain.ad.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 소재를 노출할 OS.
 * 애드픽 앱 설치형 캠페인은 OS가 갈리므로, iOS 사용자에게 Android 캠페인을 보여주면
 * 클릭해도 전환이 일어나지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum AdTargetOs {

    ALL("전체"),
    ANDROID("Android"),
    IOS("iOS");

    private final String description;

    /** 애드픽 apOS 값을 우리 enum으로 옮긴다. 모르는 값은 노출하지 않도록 비운다. */
    public static AdTargetOs fromAdpick(String apOs) {
        if (apOs == null || apOs.isBlank()) {
            return ALL;
        }
        String normalized = apOs.trim().toLowerCase();
        if (normalized.startsWith("and")) {
            return ANDROID;
        }
        if (normalized.startsWith("ios") || normalized.startsWith("iphone")) {
            return IOS;
        }
        return ALL;
    }

    /** 요청 OS에 이 타깃을 노출해도 되는지. */
    public boolean matches(AdTargetOs requested) {
        return this == ALL || requested == ALL || this == requested;
    }
}
