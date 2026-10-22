package com.swyp.picke.domain.classroom.service.debate;

import static org.assertj.core.api.Assertions.assertThat;

import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClassDebatePromptsTest {

    @ParameterizedTest
    @DisplayName("토론 입력에 클래스 수준이 한글 이름으로 들어간다")
    @CsvSource({"BEGINNER,초급", "INTERMEDIATE,중급", "ADVANCED,고급"})
    void userPromptCarriesDifficulty(BattleDifficulty difficulty, String label) {
        String prompt = ClassDebatePrompts.user(input(difficulty));

        assertThat(prompt).contains("[수준] " + label + "\n");
    }

    @Test
    @DisplayName("시스템 프롬프트에 수준별 반론 방식이 모두 있고, 권장 길이는 출력 검증 범위(50~500자) 안이다")
    void systemPromptDescribesEveryDifficulty() {
        assertThat(ClassDebatePrompts.SYSTEM)
                .contains("- 초급:", "- 중급:", "- 고급:")
                .contains("권장 120~250자", "권장 200~350자", "권장 300~500자");
        assertThat(ClassDebatePrompts.PROMPT_VERSION).isEqualTo("debate-v2");
    }

    private static DebatePromptInput input(BattleDifficulty difficulty) {
        return new DebatePromptInput(
                "표현하지 않은 감정까지 타인이 판단해도 될까요?",
                "감정 표현의 자유와 공동체의 기대를 비교해 봐요.",
                new DebatePromptInput.Side("A", "판단할 수 있다", "표현은 관계에 대한 책임의 일부다"),
                new DebatePromptInput.Side("B", "판단하기 어렵다", "내면과 표현은 다를 수 있다"),
                PhilosopherType.values()[0],
                difficulty,
                1,
                List.of());
    }
}
