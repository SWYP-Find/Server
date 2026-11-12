package com.swyp.picke.domain.classroom.service.debate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.classroom.enums.ClassDebateInputType;
import com.swyp.picke.domain.classroom.enums.ClassDebateResponseType;
import com.swyp.picke.domain.classroom.service.report.ClassReportAiClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassDebateInputClassifierTest {

    private final ClassReportAiClient aiClient = mock(ClassReportAiClient.class);
    private final ClassDebateInputClassifier classifier = new ClassDebateInputClassifier(aiClient);
    private static final ClassDebateInputClassifier.Context CONTEXT =
            new ClassDebateInputClassifier.Context("감정을 드러내지 않는 슬픔도 진짜 슬픔일까?", "진짜 슬픔이다", "직전 주장");

    private ClassDebateInputClassifier.Result classify(List<String> types, String validArgument) {
        Map<String, Object> json = new HashMap<>();
        json.put("input_types", types);
        json.put("valid_argument", validArgument);
        when(aiClient.getModel()).thenReturn("m");
        when(aiClient.chatJson(eq("m"), eq(ClassDebateInputClassifier.SYSTEM), anyString(), anyInt()))
                .thenReturn(new ClassReportAiClient.AiChatResult(json, "{}", 10, 5));
        return classifier.classify("반박", CONTEXT);
    }

    @Test
    @DisplayName("비속어가 섞여도 논거가 있으면 분석 가능, 모르는 유형 이름은 버린다")
    void profanityWithArgument() {
        ClassDebateInputClassifier.Result result = classify(List.of("profanity", "SHORT_VALID", "WHATEVER"), "논거");

        assertThat(result.types()).containsExactlyInAnyOrder(ClassDebateInputType.SHORT_VALID, ClassDebateInputType.PROFANITY);
        assertThat(result.typesValue()).isEqualTo("SHORT_VALID,PROFANITY");
        assertThat(result.validForAnalysis()).isTrue();
        assertThat(result.isInjection()).isFalse();
        assertThat(result.isMeaningless()).isFalse();
        assertThat(result.responseType()).isEqualTo(ClassDebateResponseType.NORMAL);
    }

    @Test
    @DisplayName("인젝션이 섞이면 반려, 무관·이유 부족은 주제 복귀·구체화 응답, 'null' 문자열 논거는 없음으로 본다")
    void decisions() {
        ClassDebateInputClassifier.Result injection = classify(List.of("NORMAL", "INJECTION"), "논거");
        assertThat(injection.isInjection()).isTrue();
        assertThat(injection.validForAnalysis()).isFalse();

        ClassDebateInputClassifier.Result offTopic = classify(List.of("OFF_TOPIC"), "null");
        assertThat(offTopic.responseType()).isEqualTo(ClassDebateResponseType.RETURN_TO_TOPIC);
        assertThat(offTopic.validForAnalysis()).isFalse();
        assertThat(offTopic.validArgument()).isNull();

        assertThat(classify(List.of("INSUFFICIENT"), null).responseType()).isEqualTo(ClassDebateResponseType.CLARIFY);
        assertThat(classify(List.of("MEANINGLESS"), null).isMeaningless()).isTrue();
        assertThat(classify(List.of("MEANINGLESS", "NORMAL"), "논거").isMeaningless()).isFalse();
    }

    @Test
    @DisplayName("내용 유형이 없는 응답은 형식 오류로 본다 (호출하는 쪽이 통과 처리)")
    void rejectsWithoutContentType() {
        assertThatThrownBy(() -> classify(List.of("PROFANITY"), null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> classify(List.of(), null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("반박은 구분자 안에 넣고 직전 AI 주장은 맥락으로 붙인다")
    void userPrompt() {
        String prompt = ClassDebateInputClassifier.user("내 반박", CONTEXT);
        assertThat(prompt).contains("[배틀 주제]\n감정을 드러내지 않는 슬픔도 진짜 슬픔일까?")
                .contains("[직전 AI 철학자 주장] (맥락)\n직전 주장")
                .contains("<<<\n내 반박\n>>>");
    }
}
