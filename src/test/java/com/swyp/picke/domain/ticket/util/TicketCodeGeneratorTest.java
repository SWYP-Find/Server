package com.swyp.picke.domain.ticket.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TicketCodeGeneratorTest {

    @Test
    @DisplayName("PKE + 16진수 대문자 6자리(예: PKE05D2E1)로 생성된다")
    void generatesPkePlusSixHex() {
        for (int i = 0; i < 200; i++) {
            String raw = TicketCodeGenerator.generate();
            assertThat(raw).hasSize(TicketCodeGenerator.CODE_LENGTH).matches("PKE[0-9A-F]{6}");
        }
    }

    @Test
    @DisplayName("표시 형식은 원문 그대로이고, 소문자·공백·하이픈을 섞어 입력해도 정규화하면 같은 해시가 된다")
    void formatIsReversibleThroughNormalize() {
        String raw = TicketCodeGenerator.generate();

        assertThat(TicketCodeGenerator.format(raw)).isEqualTo(raw);
        assertThat(TicketCodeHasher.normalize(" pke-" + raw.substring(3).toLowerCase() + " ")).isEqualTo(raw);
        assertThat(TicketCodeHasher.hash(raw.toLowerCase())).isEqualTo(TicketCodeHasher.hash(raw));
    }

    @Test
    @DisplayName("무작위 6자리가 고르게 나온다 (한 글자 위치에 16가지가 모두 나온다)")
    void randomPartUsesAllHexDigits() {
        Set<Character> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            seen.add(TicketCodeGenerator.generate().charAt(3));
        }
        assertThat(seen).hasSize(16);
    }
}
