package com.swyp.picke.domain.scenario.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.scenario.converter.ScenarioConverter;
import com.swyp.picke.domain.scenario.dto.request.NodeRequest;
import com.swyp.picke.domain.scenario.dto.request.ScenarioCreateRequest;
import com.swyp.picke.domain.scenario.dto.request.ScriptRequest;
import com.swyp.picke.domain.scenario.entity.Scenario;
import com.swyp.picke.domain.scenario.entity.ScenarioNode;
import com.swyp.picke.domain.scenario.entity.Script;
import com.swyp.picke.domain.scenario.enums.AudioPathType;
import com.swyp.picke.domain.scenario.enums.CreatorType;
import com.swyp.picke.domain.scenario.enums.ScenarioStatus;
import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.domain.scenario.repository.ScenarioRepository;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import com.swyp.picke.global.infra.s3.service.S3UploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScenarioServiceImplTest {

    @Mock
    private ScenarioRepository scenarioRepository;
    @Mock
    private BattleRepository battleRepository;
    @Mock
    private BattleVoteRepository battleVoteRepository;
    @Mock
    private ScenarioConverter scenarioConverter;
    @Mock
    private ScenarioAudioPipelineService audioPipelineService;
    @Mock
    private S3UploadService s3Service;
    @Mock
    private BattleOptionRepository battleOptionRepository;

    private ScenarioServiceImpl scenarioService;

    @BeforeEach
    void setUp() {
        scenarioService = new ScenarioServiceImpl(
                scenarioRepository,
                battleRepository,
                battleVoteRepository,
                scenarioConverter,
                audioPipelineService,
                s3Service,
                battleOptionRepository
        );
    }

    @Test
    void updateScenarioContent_textChanged_invalidatesOnlyChangedScriptChunk_andClearsMergedAudio() {
        Scenario scenario = createScenario();
        ScenarioNode startNode = createNode("START", true);
        Script unchangedScript = createScript(SpeakerType.NARRATOR, "NARRATOR", "line-1", "s3://chunks/script-1.mp3");
        Script changedScript = createScript(SpeakerType.NARRATOR, "NARRATOR", "line-2-old", "s3://chunks/script-2-old.mp3");
        startNode.addScript(unchangedScript);
        startNode.addScript(changedScript);
        scenario.addNode(startNode);
        scenario.addAudioUrl(AudioPathType.COMMON, "s3://merged/common-old.mp3");
        scenario.replaceVoiceSettings(Map.of(SpeakerType.NARRATOR, "voice-narrator"));

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(battleOptionRepository.findByBattle(scenario.getBattle())).thenReturn(List.of());

        ScenarioCreateRequest request = new ScenarioCreateRequest(
                1L,
                false,
                ScenarioStatus.PENDING,
                List.of(
                        new NodeRequest(
                                "START",
                                true,
                                "",
                                List.of(
                                        new ScriptRequest("NARRATOR", SpeakerType.NARRATOR, "line-1"),
                                        new ScriptRequest("NARRATOR", SpeakerType.NARRATOR, "line-2-new")
                                ),
                                List.of()
                        )
                ),
                Map.of(SpeakerType.NARRATOR, "voice-narrator")
        );

        scenarioService.updateScenarioContent(1L, request);

        assertThat(unchangedScript.getAudioUrl()).isEqualTo("s3://chunks/script-1.mp3");
        assertThat(changedScript.getAudioUrl()).isNull();
        assertThat(scenario.getAudios()).isEmpty();

        verify(s3Service).deleteFile("s3://chunks/script-2-old.mp3");
        verify(s3Service).deleteFile("s3://merged/common-old.mp3");
        verify(s3Service, never()).deleteFile("s3://chunks/script-1.mp3");
    }

    @Test
    void updateScenarioContent_voiceChanged_invalidatesOnlyAffectedSpeakerChunks_andKeepsOthers() {
        Scenario scenario = createScenario();
        ScenarioNode startNode = createNode("START", true);
        Script narratorScript = createScript(SpeakerType.NARRATOR, "NARRATOR", "same-narrator", "s3://chunks/narrator-old.mp3");
        Script aScript = createScript(SpeakerType.A, "A", "same-a", "s3://chunks/a-old.mp3");
        startNode.addScript(narratorScript);
        startNode.addScript(aScript);
        scenario.addNode(startNode);
        scenario.addAudioUrl(AudioPathType.COMMON, "s3://merged/common-old.mp3");
        scenario.replaceVoiceSettings(Map.of(
                SpeakerType.NARRATOR, "voice-narrator-v1",
                SpeakerType.A, "voice-a-v1"
        ));

        when(scenarioRepository.findById(2L)).thenReturn(Optional.of(scenario));
        when(battleOptionRepository.findByBattle(scenario.getBattle())).thenReturn(List.of());

        ScenarioCreateRequest request = new ScenarioCreateRequest(
                1L,
                false,
                ScenarioStatus.PENDING,
                List.of(
                        new NodeRequest(
                                "START",
                                true,
                                "",
                                List.of(
                                        new ScriptRequest("NARRATOR", SpeakerType.NARRATOR, "same-narrator"),
                                        new ScriptRequest("A", SpeakerType.A, "same-a")
                                ),
                                List.of()
                        )
                ),
                Map.of(
                        SpeakerType.NARRATOR, "voice-narrator-v1",
                        SpeakerType.A, "voice-a-v2"
                )
        );

        scenarioService.updateScenarioContent(2L, request);

        assertThat(narratorScript.getAudioUrl()).isEqualTo("s3://chunks/narrator-old.mp3");
        assertThat(aScript.getAudioUrl()).isNull();
        assertThat(scenario.getAudios()).isEmpty();

        verify(s3Service).deleteFile("s3://chunks/a-old.mp3");
        verify(s3Service).deleteFile("s3://merged/common-old.mp3");
        verify(s3Service, never()).deleteFile("s3://chunks/narrator-old.mp3");
    }

    private Scenario createScenario() {
        Battle battle = Battle.builder()
                .title("battle")
                .build();
        return Scenario.builder()
                .battle(battle)
                .isInteractive(false)
                .status(ScenarioStatus.PENDING)
                .creatorType(CreatorType.ADMIN)
                .build();
    }

    private ScenarioNode createNode(String nodeName, boolean startNode) {
        return ScenarioNode.builder()
                .nodeName(nodeName)
                .isStartNode(startNode)
                .audioDuration(0)
                .build();
    }

    private Script createScript(SpeakerType speakerType, String speakerName, String text, String audioUrl) {
        Script script = Script.builder()
                .startTimeMs(0)
                .speakerType(speakerType)
                .speakerName(speakerName)
                .text(text)
                .build();
        script.updateAudioUrl(audioUrl);
        return script;
    }
}
