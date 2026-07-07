package com.swyp.picke.domain.scenario.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.scenario.entity.Scenario;
import com.swyp.picke.domain.scenario.entity.ScenarioNode;
import com.swyp.picke.domain.scenario.entity.Script;
import com.swyp.picke.domain.scenario.enums.AudioPathType;
import com.swyp.picke.domain.scenario.enums.CreatorType;
import com.swyp.picke.domain.scenario.enums.ScenarioStatus;
import com.swyp.picke.domain.scenario.enums.SpeakerType;
import com.swyp.picke.domain.scenario.repository.ScenarioRepository;
import com.swyp.picke.global.infra.media.service.FFmpegService;
import com.swyp.picke.global.infra.s3.service.S3UploadService;
import com.swyp.picke.global.infra.tts.service.TtsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScenarioAudioPipelineServiceTest {

    @Mock
    private ScenarioRepository scenarioRepository;

    @Mock
    private TtsService ttsService;

    @Mock
    private S3UploadService s3UploadService;

    @Mock
    private BattleRepository battleRepository;

    private FFmpegService ffmpegService;
    private ScenarioAudioPipelineService scenarioAudioPipelineService;
    private final List<File> uploadedFiles = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ffmpegService = new FFmpegService();
        ReflectionTestUtils.setField(ffmpegService, "ffmpegPath", "/usr/bin/ffmpeg");
        ReflectionTestUtils.setField(ffmpegService, "ffprobePath", "/usr/bin/ffprobe");

        scenarioAudioPipelineService = new ScenarioAudioPipelineService(
                scenarioRepository,
                ttsService,
                ffmpegService,
                s3UploadService,
                battleRepository
        );
    }

    @AfterEach
    void tearDown() {
        for (File file : uploadedFiles) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    @Test
    void generateAndMergeAudioAsync_usesFallbackExecutableAndCompletesUploadFlow() throws Exception {
        Battle battle = Battle.builder()
                .title("scenario battle")
                .summary("summary")
                .description("description")
                .targetDate(LocalDate.now())
                .audioDuration(0)
                .status(BattleStatus.PENDING)
                .creatorType(BattleCreatorType.ADMIN)
                .build();

        Scenario scenario = Scenario.builder()
                .battle(battle)
                .isInteractive(false)
                .status(ScenarioStatus.PENDING)
                .creatorType(CreatorType.ADMIN)
                .build();

        ScenarioNode node = ScenarioNode.builder()
                .nodeName("START")
                .isStartNode(true)
                .audioDuration(0)
                .build();

        Script firstScript = Script.builder()
                .speakerType(SpeakerType.NARRATOR)
                .speakerName("Narrator")
                .text("First line")
                .build();
        Script secondScript = Script.builder()
                .speakerType(SpeakerType.NARRATOR)
                .speakerName("Narrator")
                .text("Second line")
                .build();

        node.addScript(firstScript);
        node.addScript(secondScript);
        scenario.addNode(node);
        scenario.replaceVoiceSettings(Map.of(SpeakerType.NARRATOR, "voice-narrator"));

        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(ttsService.generateTtsWithSsml(eq("First line"), eq(SpeakerType.NARRATOR), eq("voice-narrator")))
                .thenReturn(ffmpegService.createSilenceFile(1000));
        when(ttsService.generateTtsWithSsml(eq("Second line"), eq(SpeakerType.NARRATOR), eq("voice-narrator")))
                .thenReturn(ffmpegService.createSilenceFile(1000));

        AtomicInteger chunkIndex = new AtomicInteger();
        when(s3UploadService.uploadFile(anyString(), any(File.class))).thenAnswer(invocation -> {
            File file = invocation.getArgument(1, File.class);
            uploadedFiles.add(file);

            String key = invocation.getArgument(0, String.class);
            if (key.contains("/chunks/")) {
                return "s3://scenario/chunks/" + chunkIndex.incrementAndGet() + ".mp3";
            }
            return "s3://scenario/merged/common.mp3";
        });

        scenarioAudioPipelineService.generateAndMergeAudioAsync(1L);

        assertThat(scenario.getStatus()).isEqualTo(ScenarioStatus.PUBLISHED);
        assertThat(firstScript.getAudioUrl()).isEqualTo("s3://scenario/chunks/1.mp3");
        assertThat(secondScript.getAudioUrl()).isEqualTo("s3://scenario/chunks/2.mp3");
        assertThat(scenario.getAudios()).containsEntry(AudioPathType.COMMON, "s3://scenario/merged/common.mp3");
        assertThat(battle.getAudioDuration()).isNotNull().isGreaterThan(0);

        verify(ttsService, times(2)).generateTtsWithSsml(anyString(), eq(SpeakerType.NARRATOR), eq("voice-narrator"));
        verify(s3UploadService, times(3)).uploadFile(anyString(), any(File.class));
        verify(battleRepository).save(battle);
        verify(scenarioRepository).saveAndFlush(scenario);
    }
}
