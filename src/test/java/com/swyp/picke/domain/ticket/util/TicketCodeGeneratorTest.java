package com.swyp.picke.domain.ticket.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TicketCodeGeneratorTest {

    @Test
    @DisplayName("26자 Crockford Base32 (혼동 문자 I/L/O/U 없음) 로 생성된다")
    void generatesCrockfordBase32() {
        for (int i = 0; i < 200; i++) {
            String raw = TicketCodeGenerator.generate();
            assertThat(raw).hasSize(26).matches("[0-9A-HJKMNP-TV-Z]{26}");
        }
    }

    @Test
    @DisplayName("표시 형식은 6-5-5-5-5 이고, 정규화하면 원문과 같아 해시가 일치한다")
    void formatIsReversibleThroughNormalize() {
        String raw = TicketCodeGenerator.generate();
        String shown = TicketCodeGenerator.format(raw);

        assertThat(shown).matches("[0-9A-Z]{6}-[0-9A-Z]{5}-[0-9A-Z]{5}-[0-9A-Z]{5}-[0-9A-Z]{5}");
        assertThat(TicketCodeHasher.normalize(shown)).isEqualTo(raw);
        assertThat(TicketCodeHasher.hash(shown.toLowerCase())).isEqualTo(TicketCodeHasher.hash(raw));
    }

    @Test
    @DisplayName("연속 생성 시 중복이 없다")
    void noDuplicates() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            assertThat(seen.add(TicketCodeGenerator.generate())).isTrue();
        }
    }
}
