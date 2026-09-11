package com.swyp.picke.domain.battle.service;

import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleCreateRequest;
import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleOptionRequest;
import com.swyp.picke.domain.admin.dto.scenario.request.AdminScenarioCreateRequest;
import com.swyp.picke.domain.admin.dto.scenario.request.AdminScenarioNodeRequest;
import com.swyp.picke.domain.admin.dto.scenario.request.AdminScenarioOptionRequest;
import com.swyp.picke.domain.admin.dto.scenario.request.AdminScenarioScriptRequest;
import com.swyp.picke.domain.battle.dto.parse.AdminBattleParseResponse;
import com.swyp.picke.domain.battle.dto.parse.ParseWarning;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.parser.BattleScriptDocument;
import com.swyp.picke.domain.battle.parser.BattleScriptDocument.OptionMeta;
import com.swyp.picke.domain.battle.parser.BattleScriptDocument.ParsedNode;
import com.swyp.picke.domain.battle.parser.BattleScriptDocument.ParsedOption;
import com.swyp.picke.domain.battle.parser.BattleScriptDocument.ParsedScript;
import com.swyp.picke.domain.battle.parser.BattleScriptDocumentParser;
import com.swyp.picke.domain.battle.parser.EmotionClassifier;
import com.swyp.picke.domain.battle.parser.EmotionClassifier.ScriptEmotion;
import com.swyp.picke.domain.battle.parser.EmotionClassifier.ScriptLine;
import com.swyp.picke.domain.battle.parser.ScriptToneExtractor;
import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.domain.scenario.enums.Tone;
import com.swyp.picke.domain.scenario.service.PhilosopherVoiceService;
import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.tag.repository.TagRepository;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import com.swyp.picke.domain.user.enums.ValueAxis;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 붙여넣은 대본을 파싱해 배틀/시나리오 등록 페이로드로 조립한다. DB 저장은 하지 않는다(미리보기용).
 * 규격/규칙: 배틀_발행_시나리오_현행_vs_개선.md 2.4 / 2.5 / 2.6 / item 9.
 */
@Service
@RequiredArgsConstructor
public class BattleScriptParseService {

    private final BattleScriptDocumentParser documentParser;
    private final ScriptToneExtractor toneExtractor;
    private final EmotionClassifier emotionClassifier;
    private final PhilosopherVoiceService philosopherVoiceService;
    private final TagRepository tagRepository;

    @Value("${fishaudio.voice-id.narrator:}")
    private String narratorVoice;

    @Value("${fishaudio.voice-id.user:}")
    private String userVoice;

    public AdminBattleParseResponse parse(String rawText) {
        BattleScriptDocument doc = documentParser.parse(rawText);
        List<ParseWarning> warnings = new ArrayList<>(doc.warnings);

        Map<String, Long> categoryTags = tagNameToId(TagType.CATEGORY);
        Map<String, Long> philosopherTags = tagNameToId(TagType.PHILOSOPHER);
        Map<String, Long> valueTags = tagNameToId(TagType.VALUE);

        List<Long> battleTagIds = new ArrayList<>();
        if (doc.category != null) {
            Long categoryId = categoryTags.get(doc.category);
            if (categoryId != null) {
                battleTagIds.add(categoryId);
            } else {
                warnings.add(ParseWarning.of("UNKNOWN_CATEGORY_TAG",
                        "'" + doc.category + "' 카테고리 태그가 없습니다.", doc.category));
            }
        }

        List<Long> optionATagIds = resolveOptionTags(doc.optionA, "A", philosopherTags, valueTags, warnings);
        List<Long> optionBTagIds = resolveOptionTags(doc.optionB, "B", philosopherTags, valueTags, warnings);

        SpeakerBinding binding = bindSpeakers(doc, warnings);

        String openingText = firstNarration(doc, "오프닝");

        AdminBattleCreateRequest battlePayload = new AdminBattleCreateRequest(
                doc.title,
                openingText,
                null,
                null,
                null,
                null,
                null,
                BattleStatus.PENDING,
                battleTagIds,
                List.of(
                        new AdminBattleOptionRequest(nz(doc.optionA.choiceName), null,
                                binding.speakerA, null, 0, optionATagIds),
                        new AdminBattleOptionRequest(nz(doc.optionB.choiceName), null,
                                binding.speakerB, null, 1, optionBTagIds)),
                0);

        List<AdminScenarioNodeRequest> nodes = buildNodes(doc, binding, warnings);
        Map<SpeakerType, String> voiceSettings = buildVoiceSettings(binding, warnings);

        AdminScenarioCreateRequest scenarioPayload = new AdminScenarioCreateRequest(
                null, doc.interactive, com.swyp.picke.domain.scenario.enums.ScenarioStatus.PENDING,
                nodes, voiceSettings);

        Map<String, String> speakerNames = new LinkedHashMap<>();
        speakerNames.put("A", binding.speakerA);
        speakerNames.put("B", binding.speakerB);

        return new AdminBattleParseResponse(battlePayload, scenarioPayload, speakerNames, warnings);
    }

    // ---- 태그 ----

    private Map<String, Long> tagNameToId(TagType type) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Tag tag : tagRepository.findAllByTypeAndDeletedAtIsNull(type)) {
            map.put(tag.getName(), tag.getId());
        }
        return map;
    }

    private List<Long> resolveOptionTags(OptionMeta option, String label,
                                         Map<String, Long> philosopherTags, Map<String, Long> valueTags,
                                         List<ParseWarning> warnings) {
        List<Long> ids = new ArrayList<>();
        for (String keyword : option.philosopherKeywords) {
            if (PhilosopherType.fromLabel(keyword) == null) {
                warnings.add(ParseWarning.of("UNKNOWN_PHILOSOPHER_TAG",
                        label + "안 철학자 키워드 '" + keyword + "'는 철학자 유형 10인에 없습니다.", keyword));
                continue;
            }
            Long id = philosopherTags.get(keyword);
            if (id == null) {
                warnings.add(ParseWarning.of("MISSING_PHILOSOPHER_TAG",
                        label + "안 철학자 태그 '" + keyword + "'가 DB에 없습니다.", keyword));
            } else {
                ids.add(id);
            }
        }
        for (String value : option.valueTags) {
            if (ValueAxis.resolve(value).isEmpty()) {
                warnings.add(ParseWarning.of("UNKNOWN_VALUE_TAG",
                        label + "안 성향 지표 '" + value + "'는 가치관 12개 문자열에 없습니다.", value));
                continue;
            }
            Long id = valueTags.get(value);
            if (id == null) {
                warnings.add(ParseWarning.of("MISSING_VALUE_TAG",
                        label + "안 성향 태그 '" + value + "'가 DB에 없습니다.", value));
            } else {
                ids.add(id);
            }
        }
        return ids;
    }

    // ---- 발화자 A/B 바인딩 ----

    private static class SpeakerBinding {
        String speakerA;
        String speakerB;
        Map<String, SpeakerType> speakerToType = new LinkedHashMap<>();
    }

    private SpeakerBinding bindSpeakers(BattleScriptDocument doc, List<ParseWarning> warnings) {
        SpeakerBinding binding = new SpeakerBinding();

        List<String> speakers = new ArrayList<>(new LinkedHashSet<>(
                doc.nodes.stream()
                        .flatMap(n -> n.scripts.stream())
                        .map(s -> s.speaker)
                        .filter(s -> s != null)
                        .toList()));

        // 1) 선택의 시간에 명시가 있으면 그대로
        Map<String, String> explicit = new LinkedHashMap<>();
        for (ParsedNode node : doc.nodes) {
            for (ParsedOption option : node.options) {
                if (option.speaker != null) {
                    explicit.put(option.speaker.strip(), option.label);
                }
            }
        }

        Map<String, String> labelMap = explicit;
        if (labelMap.size() < 2 && speakers.size() >= 2) {
            Map<String, String> llm = emotionClassifier.bindSpeakers(
                    doc.title, nz(doc.optionA.choiceName), nz(doc.optionB.choiceName), speakers);
            if (llm.size() >= 2) {
                labelMap = llm;
            } else {
                warnings.add(ParseWarning.of("SPEAKER_BINDING_FALLBACK",
                        "발화자↔A/B 매칭을 등장 순서로 임시 배정했습니다. 미리보기에서 확인하세요.", String.join(", ", speakers)));
                labelMap = new LinkedHashMap<>();
                labelMap.put(speakers.get(0), "A");
                labelMap.put(speakers.get(1), "B");
            }
        }

        for (Map.Entry<String, String> e : labelMap.entrySet()) {
            SpeakerType type = "A".equalsIgnoreCase(e.getValue()) ? SpeakerType.A : SpeakerType.B;
            binding.speakerToType.put(e.getKey(), type);
            if (type == SpeakerType.A) {
                binding.speakerA = e.getKey();
            } else {
                binding.speakerB = e.getKey();
            }
        }
        if (binding.speakerA == null || binding.speakerB == null) {
            warnings.add(ParseWarning.blocking("INCOMPLETE_SPEAKER_BINDING",
                    "A/B 발화자를 확정하지 못했습니다. 미리보기에서 지정하세요.", null));
        }
        return binding;
    }

    // ---- 시나리오 노드 ----

    private List<AdminScenarioNodeRequest> buildNodes(BattleScriptDocument doc, SpeakerBinding binding,
                                                      List<ParseWarning> warnings) {
        List<String> order = doc.nodes.stream().map(n -> n.name).toList();
        boolean hasClosing = order.contains("클로징");

        // 배치 감정 분류: 모든 대사 줄을 인덱스로 모아 한 번 호출
        List<ScriptLine> llmInput = new ArrayList<>();
        List<int[]> indexMap = new ArrayList<>(); // [nodeIdx, scriptIdx]
        List<ScriptToneExtractor.Result> extracted = new ArrayList<>();
        int running = 0;
        for (int ni = 0; ni < doc.nodes.size(); ni++) {
            ParsedNode node = doc.nodes.get(ni);
            for (int si = 0; si < node.scripts.size(); si++) {
                ParsedScript s = node.scripts.get(si);
                ScriptToneExtractor.Result r = toneExtractor.extract(s.text);
                extracted.add(r);
                warnings.addAll(r.warnings());
                llmInput.add(new ScriptLine(running, s.speaker, r.cleanedText()));
                indexMap.add(new int[]{ni, si});
                running++;
            }
        }
        Map<Integer, ScriptEmotion> emotions = emotionClassifier.classify(doc.title, llmInput);
        if (emotions.isEmpty() && !llmInput.isEmpty()) {
            warnings.add(ParseWarning.of("LLM_CLASSIFY_FAILED",
                    "감정 자동분류에 실패했습니다. 톤은 전부 NEUTRAL 입니다. 미리보기에서 지정하세요.", null));
        }

        List<AdminScenarioNodeRequest> result = new ArrayList<>();
        int flat = 0;
        for (int ni = 0; ni < doc.nodes.size(); ni++) {
            ParsedNode node = doc.nodes.get(ni);
            List<AdminScenarioScriptRequest> scripts = new ArrayList<>();
            for (int si = 0; si < node.scripts.size(); si++) {
                ParsedScript s = node.scripts.get(si);
                ScriptToneExtractor.Result ex = extracted.get(flat);
                ScriptEmotion emotion = emotions.get(flat);
                flat++;

                Tone tone = ex.plannerTone() != Tone.NEUTRAL
                        ? ex.plannerTone()
                        : (emotion != null ? emotion.tone() : Tone.NEUTRAL);
                String text = emotion != null && emotion.textWithEffects() != null
                        ? emotion.textWithEffects()
                        : ex.cleanedText();
                SpeakerType speakerType = resolveSpeakerType(s.speaker, binding);
                String speakerName = s.speaker == null ? "나레이터" : s.speaker;
                scripts.add(new AdminScenarioScriptRequest(speakerName, speakerType, text, tone));
            }

            List<AdminScenarioOptionRequest> options = node.options.stream()
                    .map(o -> new AdminScenarioOptionRequest(o.label, o.nextNodeName))
                    .toList();

            String autoNext = resolveAutoNext(order, ni, node, hasClosing);
            result.add(new AdminScenarioNodeRequest(node.name, ni == 0, autoNext, scripts, options));
        }
        return result;
    }

    private String resolveAutoNext(List<String> order, int idx, ParsedNode node, boolean hasClosing) {
        if (!node.options.isEmpty()) {
            return null; // 선택 노드는 interactiveOptions 로 분기
        }
        if (node.name.startsWith("분기_")) {
            return hasClosing ? "클로징" : null;
        }
        for (int j = idx + 1; j < order.size(); j++) {
            String next = order.get(j);
            if (next.startsWith("분기_")) {
                return null; // 다음이 분기 블록이면 이 노드가 선택 직전이 아니어야 정상 — 방어적으로 끊음
            }
            return next;
        }
        return null;
    }

    private SpeakerType resolveSpeakerType(String speaker, SpeakerBinding binding) {
        if (speaker == null) {
            return SpeakerType.NARRATOR;
        }
        return binding.speakerToType.getOrDefault(speaker.strip(), SpeakerType.A);
    }

    // ---- 보이스 ----

    private Map<SpeakerType, String> buildVoiceSettings(SpeakerBinding binding, List<ParseWarning> warnings) {
        Map<SpeakerType, String> voices = new LinkedHashMap<>();
        putVoice(voices, SpeakerType.A, binding.speakerA, warnings);
        putVoice(voices, SpeakerType.B, binding.speakerB, warnings);
        if (narratorVoice != null && !narratorVoice.isBlank()) {
            voices.put(SpeakerType.NARRATOR, narratorVoice);
        }
        if (userVoice != null && !userVoice.isBlank()) {
            voices.put(SpeakerType.USER, userVoice);
        }
        return voices;
    }

    private void putVoice(Map<SpeakerType, String> voices, SpeakerType type, String speaker,
                          List<ParseWarning> warnings) {
        if (speaker == null) {
            return;
        }
        Optional<String> refId = philosopherVoiceService.findReferenceIdByName(speaker);
        if (refId.isPresent()) {
            voices.put(type, refId.get());
        } else {
            warnings.add(ParseWarning.blocking("MISSING_VOICE",
                    "'" + speaker + "' 보이스가 philosopher_voice 에 없습니다. 미리보기에서 지정하거나 테이블에 추가하세요.", speaker));
        }
    }

    // ---- 유틸 ----

    private String firstNarration(BattleScriptDocument doc, String nodeName) {
        return doc.nodes.stream()
                .filter(n -> n.name.equals(nodeName))
                .flatMap(n -> n.scripts.stream())
                .filter(s -> s.speaker == null)
                .map(s -> s.text)
                .findFirst()
                .orElse(null);
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }
}
