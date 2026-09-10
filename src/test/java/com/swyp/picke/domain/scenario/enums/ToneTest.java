package com.swyp.picke.domain.scenario.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ToneTest {

    @Test
    void fromInlineTag_기본_톤_태그를_해석한다() {
        assertThat(Tone.fromInlineTag("[angry]")).contains(Tone.ANGRY);
        assertThat(Tone.fromInlineTag("[excited]")).contains(Tone.EXCITED);
    }

    @Test
    void fromInlineTag_축약_표기도_흡수한다() {
        assertThat(Tone.fromInlineTag("[soft]")).contains(Tone.SOFT_TONE);
        assertThat(Tone.fromInlineTag("[soft tone]")).contains(Tone.SOFT_TONE);
        assertThat(Tone.fromInlineTag("[whisper]")).contains(Tone.WHISPERING);
    }

    @Test
    void fromInlineTag_대괄호_없이_대소문자_섞여도_해석한다() {
        assertThat(Tone.fromInlineTag(" Angry ")).contains(Tone.ANGRY);
    }

    @Test
    void fromInlineTag_효과_태그나_미지정은_empty() {
        assertThat(Tone.fromInlineTag("[sighing]")).isEmpty();
        assertThat(Tone.fromInlineTag(null)).isEmpty();
        assertThat(Tone.fromInlineTag("[break]")).isEmpty();
    }

    @Test
    void fromNameOrNeutral_null이거나_모르는_값이면_NEUTRAL() {
        assertThat(Tone.fromNameOrNeutral(null)).isEqualTo(Tone.NEUTRAL);
        assertThat(Tone.fromNameOrNeutral("")).isEqualTo(Tone.NEUTRAL);
        assertThat(Tone.fromNameOrNeutral("ANGRY")).isEqualTo(Tone.ANGRY);
        assertThat(Tone.fromNameOrNeutral("something-else")).isEqualTo(Tone.NEUTRAL);
    }

    @Test
    void NEUTRAL만_태그가_없다() {
        assertThat(Tone.NEUTRAL.hasTag()).isFalse();
        for (Tone tone : Tone.values()) {
            if (tone != Tone.NEUTRAL) {
                assertThat(tone.hasTag()).isTrue();
                assertThat(tone.getTag()).startsWith("[").endsWith("]");
            }
        }
        assertThat(Optional.ofNullable(Tone.NEUTRAL.getTag())).isEmpty();
    }
}
