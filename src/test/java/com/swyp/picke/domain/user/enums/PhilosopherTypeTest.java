package com.swyp.picke.domain.user.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PhilosopherTypeTest {

    @Test
    @DisplayName("철학자 유형은 지정된 10명만 존재한다")
    void containsOnlySupportedPhilosophers() {
        Set<String> labels = Arrays.stream(PhilosopherType.values())
                .map(PhilosopherType::getLabel)
                .collect(Collectors.toSet());

        assertThat(labels).containsExactlyInAnyOrder(
                "노자", "플라톤", "석가모니", "칸트", "아리스토텔레스",
                "마르크스", "소크라테스", "공자", "사르트르", "니체"
        );
        assertThat(PhilosopherType.values()).hasSize(10);
    }

    @Test
    @DisplayName("제거된 철학자 이름은 유형으로 처리하지 않는다")
    void doesNotResolveRemovedPhilosophers() {
        assertThat(PhilosopherType.fromLabel("데카르트")).isNull();
        assertThat(PhilosopherType.fromLabel("롤스")).isNull();
        assertThat(PhilosopherType.fromLabel("붓다")).isNull();
        assertThat(PhilosopherType.fromLabel("석가모니")).isEqualTo(PhilosopherType.BUDDHA);
    }
}
