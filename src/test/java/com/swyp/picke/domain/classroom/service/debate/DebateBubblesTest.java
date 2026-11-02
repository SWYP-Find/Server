package com.swyp.picke.domain.classroom.service.debate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DebateBubblesTest {

    @Test
    @DisplayName("모델이 줄바꿈으로 2~3개로 나눴으면 그대로 쓴다")
    void keepsModelLineBreaks() {
        assertThat(DebateBubbles.split("첫 덩어리예요.\n둘째 덩어리예요."))
                .containsExactly("첫 덩어리예요.", "둘째 덩어리예요.");
        assertThat(DebateBubbles.split("하나예요.\n\n  둘이에요.  \n셋이에요."))
                .containsExactly("하나예요.", "둘이에요.", "셋이에요.");
    }

    @Test
    @DisplayName("줄바꿈이 없으면 문장 끝에서 길이가 비슷하게 2개로 묶는다")
    void groupsSentencesIntoTwo() {
        List<String> bubbles = DebateBubbles.split("짧은 첫 문장이에요. 조금 더 긴 두 번째 문장이 이어져요. 마지막 질문은 무엇일까요?");

        assertThat(bubbles).hasSize(2);
        assertThat(String.join(" ", bubbles)).isEqualTo("짧은 첫 문장이에요. 조금 더 긴 두 번째 문장이 이어져요. 마지막 질문은 무엇일까요?");
    }

    @Test
    @DisplayName("250자를 넘고 문장이 충분하면 3개로 묶고, 4개 이상으로 나눈 응답도 3개로 다시 묶는다")
    void groupsLongTextIntoThree() {
        String sentence = "가".repeat(60) + "요.";
        String text = String.join("\n", sentence, sentence, sentence, sentence, sentence);

        List<String> bubbles = DebateBubbles.split(text);

        assertThat(bubbles).hasSize(3);
        assertThat(String.join(" ", bubbles)).isEqualTo(String.join(" ", sentence, sentence, sentence, sentence, sentence));
    }

    @Test
    @DisplayName("한 문장뿐이면 가운데에 가까운 쉼표 뒤에서, 쉼표가 없으면 띄어쓰기에서 나눠 어절을 끊지 않는다")
    void splitsSingleSentenceAtCommaOrSpace() {
        assertThat(DebateBubbles.split("친구를 위한 거짓말이라도, 모두가 같은 이유로 거짓말하면 믿음이 남을까요?"))
                .containsExactly("친구를 위한 거짓말이라도,", "모두가 같은 이유로 거짓말하면 믿음이 남을까요?");
        assertThat(DebateBubbles.split("모두가 같은 이유로 거짓말하면 믿음이 남을까요?"))
                .hasSize(2)
                .allSatisfy(b -> assertThat(b).doesNotStartWith(" ").doesNotEndWith(" "));
    }

    @Test
    @DisplayName("저장한 글을 다시 나눠도 결과가 같고, 길이는 말풍선 사이 줄바꿈을 세지 않는다")
    void joinIsStableAndLengthIgnoresLineBreaks() {
        List<String> bubbles = DebateBubbles.split("첫 문장이에요. 둘째 문장이에요. 셋째 문장이에요.");
        String stored = DebateBubbles.join(bubbles);

        assertThat(DebateBubbles.split(stored)).isEqualTo(bubbles);
        assertThat(DebateBubbles.length("가나\n다라")).isEqualTo(5);
    }
}
