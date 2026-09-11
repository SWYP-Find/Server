package com.swyp.picke.domain.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleOptionRequest;
import com.swyp.picke.domain.admin.dto.scenario.request.AdminScenarioNodeRequest;
import com.swyp.picke.domain.battle.dto.parse.AdminBattleParseResponse;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.parser.BattleScriptDocumentParser;
import com.swyp.picke.domain.battle.parser.EmotionClassifier;
import com.swyp.picke.domain.battle.parser.ScriptToneExtractor;
import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.domain.scenario.service.PhilosopherVoiceService;
import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.tag.repository.TagRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BattleScriptParseServiceTest {

    @Mock
    private EmotionClassifier emotionClassifier;
    @Mock
    private PhilosopherVoiceService philosopherVoiceService;
    @Mock
    private TagRepository tagRepository;

    private BattleScriptParseService service;

    private final AtomicLong tagIdSeq = new AtomicLong(1);

    private Tag tag(String name, TagType type) {
        Tag t = Tag.builder().name(name).type(type).build();
        ReflectionTestUtils.setField(t, "id", tagIdSeq.getAndIncrement());
        return t;
    }

    @BeforeEach
    void setUp() {
        service = new BattleScriptParseService(
                new BattleScriptDocumentParser(), new ScriptToneExtractor(),
                emotionClassifier, philosopherVoiceService, tagRepository);
        ReflectionTestUtils.setField(service, "narratorVoice", "voice-narrator");
        ReflectionTestUtils.setField(service, "userVoice", "voice-user");

        lenient().when(tagRepository.findAllByTypeAndDeletedAtIsNull(TagType.CATEGORY))
                .thenReturn(List.of(tag("철학", TagType.CATEGORY), tag("관계·사랑", TagType.CATEGORY),
                        tag("관계", TagType.CATEGORY)));
        lenient().when(tagRepository.findAllByTypeAndDeletedAtIsNull(TagType.PHILOSOPHER))
                .thenReturn(List.of(
                        tag("노자", TagType.PHILOSOPHER), tag("플라톤", TagType.PHILOSOPHER),
                        tag("석가모니", TagType.PHILOSOPHER), tag("칸트", TagType.PHILOSOPHER),
                        tag("아리스토텔레스", TagType.PHILOSOPHER), tag("마르크스", TagType.PHILOSOPHER),
                        tag("소크라테스", TagType.PHILOSOPHER), tag("공자", TagType.PHILOSOPHER),
                        tag("사르트르", TagType.PHILOSOPHER), tag("니체", TagType.PHILOSOPHER),
                        tag("노자", TagType.PHILOSOPHER)));
        lenient().when(tagRepository.findAllByTypeAndDeletedAtIsNull(TagType.VALUE))
                .thenReturn(List.of(
                        tag("내면", TagType.VALUE), tag("이상", TagType.VALUE),
                        tag("원칙", TagType.VALUE), tag("이성", TagType.VALUE),
                        tag("개인", TagType.VALUE), tag("변화", TagType.VALUE)));

        lenient().when(philosopherVoiceService.findReferenceIdByName(anyString()))
                .thenAnswer(inv -> Optional.of("voice-" + inv.getArgument(0, String.class).strip()));
        lenient().when(emotionClassifier.classify(anyString(), any())).thenReturn(Map.of());
    }

    private String load(String name) throws IOException {
        return new String(new ClassPathResource("battle-scripts/" + name).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private AdminScenarioNodeRequest node(AdminBattleParseResponse res, String name) {
        return res.scenarioPayload().nodes().stream()
                .filter(n -> n.nodeName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void 선형_대본을_배틀_페이로드로_조립한다() throws IOException {
        when(emotionClassifier.bindSpeakers(anyString(), anyString(), anyString(), any()))
                .thenReturn(Map.of("루소", "A", "홉스", "B"));

        AdminBattleParseResponse res = service.parse(load("sample-linear.txt"));

        assertThat(res.battlePayload().title()).isEqualTo("인간은 본래 선한가, 악한가?");
        assertThat(res.battlePayload().status()).isEqualTo(BattleStatus.PENDING);
        assertThat(res.battlePayload().summary()).startsWith("재난이 일어났을 때");
        assertThat(res.battlePayload().tagIds()).hasSize(1); // 철학
        assertThat(res.battlePayload().options()).extracting(AdminBattleOptionRequest::title)
                .containsExactly("선하다", "악하다");
        assertThat(res.battlePayload().options().get(0).representative()).isEqualTo("루소");
        assertThat(res.battlePayload().options().get(1).representative()).isEqualTo("홉스");
        // A안: 철학자 3 + 성향 2 = 5개 태그
        assertThat(res.battlePayload().options().get(0).tagIds()).hasSize(5);
    }

    @Test
    void 선형_대본_시나리오_노드가_오프닝_라운드_클로징_체인으로_연결된다() throws IOException {
        when(emotionClassifier.bindSpeakers(anyString(), anyString(), anyString(), any()))
                .thenReturn(Map.of("루소", "A", "홉스", "B"));

        AdminBattleParseResponse res = service.parse(load("sample-linear.txt"));

        assertThat(res.scenarioPayload().isInteractive()).isFalse();
        assertThat(res.scenarioPayload().nodes()).extracting(AdminScenarioNodeRequest::nodeName)
                .containsExactly("오프닝", "라운드", "클로징");
        assertThat(node(res, "오프닝").isStartNode()).isTrue();
        assertThat(node(res, "오프닝").autoNextNode()).isEqualTo("라운드");
        assertThat(node(res, "라운드").autoNextNode()).isEqualTo("클로징");
        assertThat(node(res, "클로징").autoNextNode()).isNull();
        assertThat(node(res, "라운드").scripts().get(0).speakerType()).isEqualTo(SpeakerType.A);
        assertThat(node(res, "오프닝").scripts().get(0).speakerType()).isEqualTo(SpeakerType.NARRATOR);
    }

    @Test
    void 선형_대본_보이스가_발화자별로_채워진다() throws IOException {
        when(emotionClassifier.bindSpeakers(anyString(), anyString(), anyString(), any()))
                .thenReturn(Map.of("루소", "A", "홉스", "B"));

        AdminBattleParseResponse res = service.parse(load("sample-linear.txt"));

        Map<SpeakerType, String> voices = res.scenarioPayload().voiceSettings();
        assertThat(voices).containsEntry(SpeakerType.A, "voice-루소")
                .containsEntry(SpeakerType.B, "voice-홉스")
                .containsEntry(SpeakerType.NARRATOR, "voice-narrator");
        assertThat(res.warnings()).noneMatch(w -> w.blocking());
    }

    @Test
    void 인터랙티브_대본은_선택_노드와_분기_연결을_만든다() throws IOException {
        AdminBattleParseResponse res = service.parse(load("sample-interactive.txt"));

        assertThat(res.scenarioPayload().isInteractive()).isTrue();
        assertThat(res.scenarioPayload().nodes()).extracting(AdminScenarioNodeRequest::nodeName)
                .containsExactly("오프닝", "1라운드", "2라운드", "선택", "분기_A", "분기_B", "클로징");

        AdminScenarioNodeRequest choice = node(res, "선택");
        assertThat(choice.autoNextNode()).isNull();
        assertThat(choice.interactiveOptions()).extracting(o -> o.label()).containsExactly("A", "B");
        assertThat(choice.interactiveOptions()).extracting(o -> o.nextNodeName())
                .containsExactly("분기_A", "분기_B");
        assertThat(node(res, "분기_A").autoNextNode()).isEqualTo("클로징");
        assertThat(node(res, "분기_B").autoNextNode()).isEqualTo("클로징");
    }

    @Test
    void 인터랙티브_대본은_선택의_시간_명시로_A_B_발화자를_바인딩한다() throws IOException {
        AdminBattleParseResponse res = service.parse(load("sample-interactive.txt"));

        assertThat(res.speakerNames()).containsEntry("A", "칸트").containsEntry("B", "아리스토텔레스");
        assertThat(node(res, "1라운드").scripts().get(0).speakerType()).isEqualTo(SpeakerType.A);   // 칸트
        assertThat(node(res, "1라운드").scripts().get(1).speakerType()).isEqualTo(SpeakerType.B);   // 아리스토텔레스
    }

    @Test
    void 실제_대본을_인터랙티브_페이로드로_조립한다() throws IOException {
        AdminBattleParseResponse res = service.parse(load("sample-real-1.txt"));

        assertThat(res.battlePayload().title()).isEqualTo("첫 데이트 국밥, 무죄인가 유죄인가?");
        assertThat(res.battlePayload().tagIds()).hasSize(1); // 관계·사랑
        assertThat(res.speakerNames()).containsEntry("A", "플라톤").containsEntry("B", "마르크스");
        assertThat(res.scenarioPayload().isInteractive()).isTrue();
        assertThat(res.scenarioPayload().nodes()).extracting(AdminScenarioNodeRequest::nodeName)
                .containsExactly("오프닝", "1라운드", "2라운드", "선택", "분기_A", "분기_B", "클로징");
        assertThat(node(res, "선택").interactiveOptions()).extracting(o -> o.nextNodeName())
                .containsExactly("분기_A", "분기_B");
        assertThat(node(res, "분기_A").autoNextNode()).isEqualTo("클로징");
        assertThat(res.scenarioPayload().voiceSettings())
                .containsEntry(SpeakerType.A, "voice-플라톤")
                .containsEntry(SpeakerType.B, "voice-마르크스");
        assertThat(res.warnings()).noneMatch(w -> w.blocking());
        // 옵션 태그: 철학자 3 + 성향 3 = 6
        assertThat(res.battlePayload().options().get(0).tagIds()).hasSize(6);
    }

    @Test
    void 사전투표_대표발화자로_A_B를_바인딩하고_미지원_성향지표는_경고한다() throws IOException {
        AdminBattleParseResponse res = service.parse(load("sample-real-3.txt"));

        // 사전 투표: 공자→A, 노자→B (LLM 호출 없이)
        assertThat(res.speakerNames()).containsEntry("A", "공자").containsEntry("B", "노자");
        assertThat(res.scenarioPayload().isInteractive()).isFalse();
        assertThat(res.scenarioPayload().nodes()).extracting(AdminScenarioNodeRequest::nodeName)
                .containsExactly("오프닝", "1라운드", "2라운드", "클로징");
        // 12축 밖 "자연" 은 태그로 안 들어가고 경고
        assertThat(res.warnings()).anyMatch(w -> w.code().equals("UNKNOWN_VALUE_TAG")
                && "자연".equals(w.context()));
        // B안 태그: 철학자 3 + 성향 2(개인,내면; 자연 제외) = 5
        assertThat(res.battlePayload().options().get(1).tagIds()).hasSize(5);
    }

    @Test
    void LLM_실패시_톤은_전부_NEUTRAL이고_경고를_남긴다() throws IOException {
        AdminBattleParseResponse res = service.parse(load("sample-interactive.txt"));

        List<com.swyp.picke.domain.scenario.enums.Tone> tones = new ArrayList<>();
        res.scenarioPayload().nodes().forEach(n -> n.scripts().forEach(s -> tones.add(s.tone())));
        assertThat(tones).containsOnly(com.swyp.picke.domain.scenario.enums.Tone.NEUTRAL);
        assertThat(res.warnings()).anyMatch(w -> w.code().equals("LLM_CLASSIFY_FAILED"));
    }
}
