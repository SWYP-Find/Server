package com.swyp.picke.domain.classroom.service.debate;

import static org.assertj.core.api.Assertions.assertThat;

import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.classroom.enums.ClassDebateSpeaker;
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

        assertThat(prompt).contains("[수준] " + label + "\n")
                .contains("[분량] 줄바꿈을 빼고 최소 200자 이상");
    }

    @ParameterizedTest
    @DisplayName("문체는 초급 해요체, 중급·고급 합니다체로 토론 입력에 들어간다")
    @CsvSource({"BEGINNER,~해요", "INTERMEDIATE,~합니다", "ADVANCED,~합니다"})
    void userPromptCarriesSpeechStyle(BattleDifficulty difficulty, String style) {
        assertThat(ClassDebatePrompts.user(input(difficulty))).contains("[말투] \"" + style + "\" 체");
    }

    @Test
    @DisplayName("시스템 프롬프트에 수준별 표현 기준이 모두 있고, 길이는 수준과 관계없이 200~500자다")
    void systemPromptDescribesEveryDifficulty() {
        assertThat(ClassDebatePrompts.SYSTEM)
                .contains("- 초급(중학생):", "- 중급(고등학생):", "- 고급(성인):")
                .contains("줄바꿈을 빼고 최소 200자 이상, 500자 이하")
                .doesNotContain("권장 200~300자");
        assertThat(ClassDebatePrompts.PROMPT_VERSION).isEqualTo("debate-v4");
    }

    @Test
    @DisplayName("토론 입력에 철학자 프로필(핵심 관점·말투·피할 표현·성향 등급)이 들어가고 점수 숫자는 넣지 않는다")
    void userPromptCarriesPhilosopherProfile() {
        String prompt = ClassDebatePrompts.user(input(BattleDifficulty.BEGINNER));

        assertThat(prompt)
                .contains("[철학자 프로필] 칸트 · 원칙형")
                .contains("- 핵심 관점: 보편적으로 적용할 원칙과 인간의 존엄을 중시한다.")
                .contains("- 말투·논증: 정확한 원칙형.")
                .contains("- 피할 표현: 관습·사규·상사의 지시를")
                .contains("- 성향 등급: 대표=원칙 / 주요=이성, 개인, 이상 / 보조=내면 / 최소=변화")
                .doesNotContain("90");
    }

    @Test
    @DisplayName("이전 AI 주장의 말풍선 줄바꿈은 대화 기록에서 한 줄로 이어 붙인다")
    void historyFlattensBubbleLineBreaks() {
        DebatePromptInput base = input(BattleDifficulty.INTERMEDIATE);
        DebatePromptInput input = new DebatePromptInput(base.question(), base.description(), base.participant(), base.opponent(),
                base.philosopher(), base.difficulty(), 2,
                List.of(new DebatePromptInput.Turn(1, ClassDebateSpeaker.AI, "첫 덩어리예요.\n둘째 덩어리예요."),
                        new DebatePromptInput.Turn(1, ClassDebateSpeaker.USER, "반박이에요.")));

        assertThat(ClassDebatePrompts.user(input)).contains("(1라운드 AI 주장) 첫 덩어리예요. 둘째 덩어리예요.\n");
    }

    private static DebatePromptInput input(BattleDifficulty difficulty) {
        return new DebatePromptInput(
                "표현하지 않은 감정까지 타인이 판단해도 될까요?",
                "감정 표현의 자유와 공동체의 기대를 비교해 봐요.",
                new DebatePromptInput.Side("A", "판단할 수 있다", "표현은 관계에 대한 책임의 일부다"),
                new DebatePromptInput.Side("B", "판단하기 어렵다", "내면과 표현은 다를 수 있다"),
                PhilosopherType.KANT,
                difficulty,
                1,
                List.of());
    }
}
