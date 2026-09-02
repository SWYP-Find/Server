package com.swyp.picke.domain.ad.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdTargetOsTest {

    @Test
    @DisplayName("애드픽 apOS 값을 우리 enum으로 옮긴다")
    void fromAdpick() {
        assertThat(AdTargetOs.fromAdpick("Android")).isEqualTo(AdTargetOs.ANDROID);
        assertThat(AdTargetOs.fromAdpick("android")).isEqualTo(AdTargetOs.ANDROID);
        assertThat(AdTargetOs.fromAdpick("iOS")).isEqualTo(AdTargetOs.IOS);
        assertThat(AdTargetOs.fromAdpick("iPhone")).isEqualTo(AdTargetOs.IOS);
    }

    @Test
    @DisplayName("OS가 비었거나 모르는 값이면 전체 노출로 둔다")
    void fromAdpick_unknown() {
        assertThat(AdTargetOs.fromAdpick(null)).isEqualTo(AdTargetOs.ALL);
        assertThat(AdTargetOs.fromAdpick("")).isEqualTo(AdTargetOs.ALL);
        assertThat(AdTargetOs.fromAdpick("Tizen")).isEqualTo(AdTargetOs.ALL);
    }

    @Test
    @DisplayName("ALL 소재는 어느 OS에나 나가고, OS 지정 소재는 같은 OS에만 나간다")
    void matches() {
        assertThat(AdTargetOs.ALL.matches(AdTargetOs.IOS)).isTrue();
        assertThat(AdTargetOs.ANDROID.matches(AdTargetOs.ANDROID)).isTrue();
        assertThat(AdTargetOs.ANDROID.matches(AdTargetOs.IOS)).isFalse();
        assertThat(AdTargetOs.IOS.matches(AdTargetOs.ANDROID)).isFalse();
    }

    @Test
    @DisplayName("요청 OS를 모르면(ALL) 모든 소재를 후보로 둔다")
    void matches_requestAll() {
        assertThat(AdTargetOs.ANDROID.matches(AdTargetOs.ALL)).isTrue();
        assertThat(AdTargetOs.IOS.matches(AdTargetOs.ALL)).isTrue();
    }
}
