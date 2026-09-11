package com.swyp.picke.domain.battle.parser;

import com.swyp.picke.domain.scenario.enums.Tone;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 대본 대사에 LLM 으로 감정 톤과 효과 태그를 붙인다. 실패해도 파싱을 막지 않는다(빈 결과 반환).
 */
public interface EmotionClassifier {

    record ScriptLine(int index, String speaker, String text) {}

    record ScriptEmotion(Tone tone, String textWithEffects) {}

    /** index -> 분류 결과. 호출 실패 시 빈 맵. */
    Map<Integer, ScriptEmotion> classify(String battleTitle, List<ScriptLine> lines);

    /** 발화자 이름 -> "A" | "B". 선택의 시간에 명시가 없을 때 논지↔선택지명칭 매칭. 실패 시 빈 맵. */
    Map<String, String> bindSpeakers(String battleTitle, String optionATitle, String optionBTitle,
                                     List<String> speakers);

    /** 오프닝 전문을 배틀 카드용 한 줄 요약으로 압축한다. 실패 시 empty. */
    Optional<String> summarizeOpening(String battleTitle, String openingText);
}
