package com.swyp.picke.domain.ticket.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketCodeHasherTest {

    @Test
    @DisplayName("공백·하이픈·대소문자가 달라도 같은 코드로 본다")
    void normalize_ignoresFormatting() {
        assertThat(TicketCodeHasher.normalize(" picke-ab12 cd34 ")).isEqualTo("PICKEAB12CD34");
        assertThat(TicketCodeHasher.hash("picke-ab12-cd34")).isEqualTo(TicketCodeHasher.hash("PICKEAB12CD34"));
    }

    @Test
    @DisplayName("해시는 SHA-256 소문자 hex 64자이고 코드가 다르면 값도 다르다")
    void hash_isSha256Hex() {
        String hash = TicketCodeHasher.hash("PICKE-AB12-CD34");

        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
        assertThat(hash).isNotEqualTo(TicketCodeHasher.hash("PICKE-AB12-CD35"));
    }

    @Test
    @DisplayName("null 코드는 빈 문자열로 정규화한다")
    void normalize_null() {
        assertThat(TicketCodeHasher.normalize(null)).isEmpty();
    }
}
