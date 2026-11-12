package com.swyp.picke.domain.classroom.service.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportProfanityMaskerTest {

    @Test
    @DisplayName("비속어는 *** 로 가리고 나머지 논거는 그대로 둔다")
    void masksProfanity() {
        assertThat(ReportProfanityMasker.mask("씨발 진짜 이건 아니지, 사람마다 슬픔을 견디는 방식이 다른 건데 개같네"))
                .isEqualTo("*** 진짜 이건 아니지, 사람마다 슬픔을 견디는 방식이 다른 건데 ***네");
        assertThat(ReportProfanityMasker.mask("그건 개 소리고 ㅅㅂ 존나 별로")).isEqualTo("그건 ***고 *** *** 별로");
    }

    @Test
    @DisplayName("일반 단어(시발점, 새끼손가락)와 비속어 없는 글은 바꾸지 않는다")
    void keepsNormalWords() {
        assertThat(ReportProfanityMasker.mask("이 논쟁의 시발점은 새끼손가락 약속이었어요")).isEqualTo("이 논쟁의 시발점은 새끼손가락 약속이었어요");
        assertThat(ReportProfanityMasker.mask(null)).isNull();
    }
}
