package com.swyp.picke.global.infra.tts.service;

import com.swyp.picke.domain.scenario.enums.Tone;
import java.util.Map;

/**
 * TTS 전송 직전 대사 텍스트를 정리한다.
 *
 * <ul>
 *   <li>줄의 대표 톤({@link Tone})을 Fish Audio S2/S2.1 인라인 표기로 맨 앞에 붙인다. {@code NEUTRAL} 은 아무것도 안 붙임.</li>
 *   <li>기획자가 관용적으로 쓰는 효과 태그 표기를 Fish 정식 표기로 정규화한다({@code [pause]} → {@code [break]} 등).</li>
 *   <li>대괄호 태그 자체는 제거하지 않는다 — Fish S2.1 이 인라인으로 해석한다.</li>
 * </ul>
 */
public final class TtsTextNormalizer {

    private static final Map<String, String> EFFECT_ALIASES = Map.of(
            "[soft]", "[soft tone]",
            "[pause]", "[break]",
            "[long pause]", "[long-break]",
            "[long-pause]", "[long-break]"
    );

    private TtsTextNormalizer() {
    }

    public static String normalize(String rawText, Tone tone) {
        String text = normalizeEffectTags(rawText);

        if (tone != null && tone.hasTag()) {
            text = tone.getTag() + " " + text.stripLeading();
        }
        return text.trim();
    }

    /** 효과 태그 표기만 Fish 정식 표기로 바꾼다(톤 태그는 안 붙임). 파서가 저장 텍스트를 정리할 때 쓴다. */
    public static String normalizeEffectTags(String rawText) {
        String text = rawText == null ? "" : rawText;
        for (Map.Entry<String, String> alias : EFFECT_ALIASES.entrySet()) {
            text = text.replaceAll("(?i)" + java.util.regex.Pattern.quote(alias.getKey()), alias.getValue());
        }
        return text;
    }
}
