package com.swyp.picke.domain.scenario.enums;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;

/**
 * 대사 한 줄의 감성적인 톤. 줄당 하나. 오디오 효과 태그(문장 중간 삽입, 다중)는 이 enum이 아니라
 * Script.text 인라인에 그대로 둔다.
 *
 * <p>{@link #tag} 는 Fish Audio S2/S2.1 이 인식하는 인라인 대괄호 표기. TTS 합성 직전 text 앞에 붙인다.
 * {@link #NEUTRAL} 은 아무 태그도 붙이지 않는다.
 */
@Getter
public enum Tone {
    NEUTRAL(null),
    ANGRY("[angry]"),
    SAD("[sad]"),
    EMBARRASSED("[embarrassed]"),
    EMPHASIS("[emphasis]"),
    WHISPERING("[whispering]"),
    SOFT_TONE("[soft tone]"),
    BREATHY("[breathy]"),
    EXCITED("[excited]");

    private final String tag;

    Tone(String tag) {
        this.tag = tag;
    }

    public boolean hasTag() {
        return tag != null;
    }

    /**
     * 기획자가 대본에 인라인으로 써넣은 톤 태그 문자열(예: {@code [angry]}, {@code [soft]})을 enum 으로 해석.
     * 표기 흔들림({@code [soft]} → SOFT_TONE)도 흡수한다. 톤이 아닌 태그면 empty.
     */
    public static Optional<Tone> fromInlineTag(String rawTag) {
        if (rawTag == null) {
            return Optional.empty();
        }
        String normalized = rawTag.trim().toLowerCase()
                .replace("[", "").replace("]", "").trim();
        return switch (normalized) {
            case "angry" -> Optional.of(ANGRY);
            case "sad" -> Optional.of(SAD);
            case "embarrassed" -> Optional.of(EMBARRASSED);
            case "emphasis" -> Optional.of(EMPHASIS);
            case "whispering", "whisper" -> Optional.of(WHISPERING);
            case "soft", "soft tone" -> Optional.of(SOFT_TONE);
            case "breathy" -> Optional.of(BREATHY);
            case "excited" -> Optional.of(EXCITED);
            default -> Optional.empty();
        };
    }

    public static Tone fromNameOrNeutral(String name) {
        if (name == null || name.isBlank()) {
            return NEUTRAL;
        }
        return Arrays.stream(values())
                .filter(tone -> tone.name().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(NEUTRAL);
    }
}
