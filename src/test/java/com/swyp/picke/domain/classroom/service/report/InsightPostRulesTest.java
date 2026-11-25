package com.swyp.picke.domain.classroom.service.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InsightPostRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {"ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ", "ㅎㅎㅎ ㅠㅠ", "ㅁㄴㅇㄹ", "몰라요 그냥요", "asdfasdfasdf", "11111111", "12341234",
            "아아아아아아아아아아", "하하하하하하하하", "ㅋㅋㅋㅋ 인정 ㅋㅋ", "!!!???...", "👍👍👍👍", "", "   "})
    @DisplayName("#657 웃음·자모·같은 글자 반복·숫자만·짧은 글은 이유가 없는 글이다")
    void noReason(String content) {
        assertThat(InsightPostRules.hasReason(content)).isFalse();
        assertThat(InsightPostRules.hasContent(content)).isFalse();
        assertThat(InsightPostRules.isSelectable(content)).isFalse();
        assertThat(InsightPostRules.isAutoSelectable(content)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"비용보다 기대 효과가 커서 찬성해요.", "표본이 작아서 효과를 믿기 어려워요 ㅋㅋ", "AI is just a tool for students"})
    @DisplayName("#657 이유를 읽을 수 있는 글은 근거로 고를 수 있다")
    void hasReason(String content) {
        assertThat(InsightPostRules.hasReason(content)).isTrue();
        assertThat(InsightPostRules.isSelectable(content)).isTrue();
        assertThat(InsightPostRules.isAutoSelectable(content)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"씨발 이딴 거 왜 함 AI 쓰면 안 됨", "병신같은 주장이라 반대합니다 진짜로"})
    @DisplayName("#657 비속어가 든 글은 AI 입력에는 넣되(가려서) 근거·대표 의견으로는 고르지 않는다")
    void profanityNotSelectable(String content) {
        assertThat(InsightPostRules.hasReason(content)).isTrue();
        assertThat(InsightPostRules.isSelectable(content)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"싸고 좋은게 짱.", "반띵 해야지", "마니마니 챙기자", "환경이 먼저"})
    @DisplayName("#717 짧아도 입장이 있는 글은 AI 입력·AI 가 고른 대표 의견에 쓰고, 서버가 채우는 대표 의견에는 쓰지 않는다")
    void shortContent(String content) {
        assertThat(InsightPostRules.hasContent(content)).isTrue();
        assertThat(InsightPostRules.isSelectable(content)).isTrue();
        assertThat(InsightPostRules.hasReason(content)).isFalse();
        assertThat(InsightPostRules.isAutoSelectable(content)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"시발!", "아잇", "ㅇㅇ", "젠장!", "저도ㅛㅇ", "몰라", "그냥요 ㅋㅋ", "asdf", "qwer qwer", "1234", "ㅋㅋ 인정"})
    @DisplayName("#717 아주 짧은 글·이유 없다는 말·영문 자판 누르기는 짧은 글 기준에서도 뺀다")
    void tooShort(String content) {
        assertThat(InsightPostRules.hasContent(content)).isFalse();
    }
}
