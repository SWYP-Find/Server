package com.swyp.picke.domain.classroom.service.debate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swyp.picke.domain.classroom.enums.ClassDebateFocus;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassDebateContentValidatorTest {

    private final ClassDebateContentValidator validator = new ClassDebateContentValidator();

    @Test
    @DisplayName("통과한 주장은 말풍선 단위 줄바꿈으로 정리해 돌려준다")
    void normalizesBubbles() {
        String first = "가".repeat(120) + "요.";
        String second = "나".repeat(120) + "요?";

        ClassDebateContentValidator.Argument argument = validator.parse(Map.of(
                "argument_text", first + "\n\n" + second, "focus", "ASSUMPTION"));

        assertThat(argument.text()).isEqualTo(first + "\n" + second);
        assertThat(argument.focus()).isEqualTo(ClassDebateFocus.ASSUMPTION);
    }

    @Test
    @DisplayName("길이는 말풍선 줄바꿈을 빼고 120~500자로 검사하고, 보정 안내는 프롬프트 기준 200자로 한다")
    void lengthIgnoresBubbleLineBreaks() {
        String ok = "가".repeat(59) + "\n" + "나".repeat(60);
        assertThat(validator.parse(Map.of("argument_text", ok, "focus", "EVIDENCE")).text()).contains("\n");

        String tooShort = "가".repeat(58) + "\n" + "나".repeat(60);
        assertThatThrownBy(() -> validator.parse(Map.of("argument_text", tooShort, "focus", "EVIDENCE")))
                .isInstanceOf(DebateValidationException.class)
                .hasFieldOrPropertyWithValue("code", "ARGUMENT_LENGTH")
                .hasFieldOrPropertyWithValue("problem", "argument_text 는 줄바꿈을 빼고 200자 이상 500자 이하로 다시 씁니다 (현재 119자)");
    }

    @Test
    @DisplayName("철학자 유형 10명 모두 토론 프로필이 있다")
    void everyPhilosopherHasProfile() {
        assertThat(Arrays.stream(PhilosopherType.values()).map(DebatePhilosopherProfile::of))
                .hasSize(PhilosopherType.values().length)
                .doesNotHaveDuplicates();
    }
}
