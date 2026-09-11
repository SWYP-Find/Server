package com.swyp.picke.domain.battle.parser;

import com.swyp.picke.domain.battle.dto.parse.ParseWarning;
import com.swyp.picke.domain.scenario.enums.Tone;
import com.swyp.picke.global.infra.tts.service.TtsTextNormalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 감정 큐 하이브리드 규칙(배틀_발행_시나리오_현행_vs_개선.md item 9)의 파서 쪽 처리.
 *
 * <ul>
 *   <li>줄에 톤 태그({@code [angry]} 등)가 있으면 첫 번째를 {@link Tone} 으로 뽑고 text 에서 톤 태그를 모두 제거한다.</li>
 *   <li>여러 톤 태그가 있으면 첫 번째만 쓰고 warning.</li>
 *   <li>효과 태그({@code [sighing]} 등)는 그대로 두되 표기를 정규화한다.</li>
 * </ul>
 */
@Component
public class ScriptToneExtractor {

    private static final Pattern BRACKET = Pattern.compile("\\[[^\\]]+\\]");

    public record Result(Tone plannerTone, String cleanedText, List<ParseWarning> warnings) {}

    public Result extract(String rawText) {
        List<ParseWarning> warnings = new ArrayList<>();
        String text = rawText == null ? "" : rawText;

        List<Tone> toneTags = new ArrayList<>();
        Matcher m = BRACKET.matcher(text);
        while (m.find()) {
            Tone.fromInlineTag(m.group()).ifPresent(toneTags::add);
        }

        Tone plannerTone = Tone.NEUTRAL;
        if (!toneTags.isEmpty()) {
            plannerTone = toneTags.get(0);
            if (toneTags.size() > 1) {
                warnings.add(ParseWarning.of("MULTIPLE_TONE_TAGS",
                        "톤 태그가 여러 개라 첫 번째(" + plannerTone + ")만 사용합니다.", rawText));
            }
            // 톤 태그는 모두 제거(효과 태그는 남긴다)
            StringBuilder sb = new StringBuilder();
            Matcher rm = BRACKET.matcher(text);
            int last = 0;
            while (rm.find()) {
                if (Tone.fromInlineTag(rm.group()).isPresent()) {
                    sb.append(text, last, rm.start());
                    last = rm.end();
                }
            }
            sb.append(text.substring(last));
            text = sb.toString();
        }

        String cleaned = TtsTextNormalizer.normalizeEffectTags(text).replaceAll("\\s{2,}", " ").trim();
        return new Result(plannerTone, cleaned, warnings);
    }
}
