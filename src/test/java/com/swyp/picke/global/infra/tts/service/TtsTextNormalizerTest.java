package com.swyp.picke.global.infra.tts.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.swyp.picke.domain.scenario.enums.Tone;
import org.junit.jupiter.api.Test;

class TtsTextNormalizerTest {

    @Test
    void NEUTRAL_톤은_태그를_붙이지_않는다() {
        String result = TtsTextNormalizer.normalize("그건 궤변이다.", Tone.NEUTRAL);

        assertThat(result).isEqualTo("그건 궤변이다.");
    }

    @Test
    void 톤이_있으면_Fish_인라인_표기를_맨_앞에_붙인다() {
        String result = TtsTextNormalizer.normalize("그건 궤변이다.", Tone.ANGRY);

        assertThat(result).isEqualTo("[angry] 그건 궤변이다.");
    }

    @Test
    void SOFT_TONE은_soft_tone_표기로_붙는다() {
        String result = TtsTextNormalizer.normalize("괜찮아요.", Tone.SOFT_TONE);

        assertThat(result).isEqualTo("[soft tone] 괜찮아요.");
    }

    @Test
    void 관용_효과_표기를_Fish_정식_표기로_정규화한다() {
        String result = TtsTextNormalizer.normalize(
                "그래서 [pause] 나는 [soft] 포기했다 [long pause] 결국.", Tone.NEUTRAL);

        assertThat(result).isEqualTo("그래서 [break] 나는 [soft tone] 포기했다 [long-break] 결국.");
    }

    @Test
    void 효과_정규화는_대소문자를_무시한다() {
        String result = TtsTextNormalizer.normalize("음 [PAUSE] 그리고 [Long Pause] 끝.", Tone.NEUTRAL);

        assertThat(result).isEqualTo("음 [break] 그리고 [long-break] 끝.");
    }

    @Test
    void 문서에_없는_효과_태그는_그대로_둔다() {
        String result = TtsTextNormalizer.normalize("[sighing] 나는 지쳤다.", Tone.NEUTRAL);

        assertThat(result).isEqualTo("[sighing] 나는 지쳤다.");
    }

    @Test
    void 톤과_효과가_함께_있으면_톤은_앞_효과는_원위치() {
        String result = TtsTextNormalizer.normalize("나는 [sighing] 포기했다.", Tone.SAD);

        assertThat(result).isEqualTo("[sad] 나는 [sighing] 포기했다.");
    }

    @Test
    void null_텍스트도_톤_태그만_남기고_안전하게_처리한다() {
        String result = TtsTextNormalizer.normalize(null, Tone.EXCITED);

        assertThat(result).isEqualTo("[excited]");
    }
}
