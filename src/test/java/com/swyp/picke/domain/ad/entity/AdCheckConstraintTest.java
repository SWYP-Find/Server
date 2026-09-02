package com.swyp.picke.domain.ad.entity;

import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdSource;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.enums.AdTargetOs;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hibernate.annotations.Check;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * enum에 값을 추가하고 CHECK 제약 갱신을 잊으면 운영에서 INSERT가 조용히 막힌다.
 * 제약 문자열과 enum 상수가 어긋나는 순간 여기서 먼저 깨지게 한다.
 */
class AdCheckConstraintTest {

    private static final Pattern QUOTED = Pattern.compile("'([^']+)'");

    private Set<String> valuesOf(Class<?> entity, String constraintName) {
        String constraints = Arrays.stream(entity.getAnnotationsByType(Check.class))
                .filter(check -> check.name().equals(constraintName))
                .map(Check::constraints)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        entity.getSimpleName() + "에 " + constraintName + " CHECK 제약이 없습니다."));

        Matcher matcher = QUOTED.matcher(constraints);
        return matcher.results()
                .map(result -> result.group(1))
                .collect(Collectors.toSet());
    }

    private Set<String> namesOf(Class<? extends Enum<?>> type) {
        return Arrays.stream(type.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("ad_creatives의 network CHECK 제약이 AdNetwork 전체와 일치한다")
    void adCreativesNetworkCheckMatchesEnum() {
        assertThat(valuesOf(AdCreative.class, "ck_ad_creatives_network"))
                .isEqualTo(namesOf(AdNetwork.class));
    }

    @Test
    @DisplayName("ad_creatives의 status CHECK 제약이 AdStatus 전체와 일치한다")
    void adCreativesStatusCheckMatchesEnum() {
        assertThat(valuesOf(AdCreative.class, "ck_ad_creatives_status"))
                .isEqualTo(namesOf(AdStatus.class));
    }

    @Test
    @DisplayName("ad_creatives의 source CHECK 제약이 AdSource 전체와 일치한다")
    void adCreativesSourceCheckMatchesEnum() {
        assertThat(valuesOf(AdCreative.class, "ck_ad_creatives_source"))
                .isEqualTo(namesOf(AdSource.class));
    }

    @Test
    @DisplayName("ad_creatives의 target_os CHECK 제약이 AdTargetOs 전체와 일치한다")
    void adCreativesTargetOsCheckMatchesEnum() {
        assertThat(valuesOf(AdCreative.class, "ck_ad_creatives_target_os"))
                .isEqualTo(namesOf(AdTargetOs.class));
    }

    @Test
    @DisplayName("slot CHECK 제약이 세 테이블 모두 AdSlotCode 전체와 일치한다")
    void slotChecksMatchEnum() {
        Set<String> slots = namesOf(AdSlotCode.class);

        assertThat(valuesOf(AdCreative.class, "ck_ad_creatives_slot")).isEqualTo(slots);
        assertThat(valuesOf(AdClickLog.class, "ck_ad_click_logs_slot")).isEqualTo(slots);
        assertThat(valuesOf(AdImpressionDaily.class, "ck_ad_impression_daily_slot")).isEqualTo(slots);
    }
}
