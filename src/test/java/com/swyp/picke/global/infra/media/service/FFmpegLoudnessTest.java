package com.swyp.picke.global.infra.media.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 대사 음성 음량 맞추기 (#701). 실제 ffmpeg 로 크기가 다른 소리를 만들어 확인한다 */
class FFmpegLoudnessTest {

    private final FFmpegService ffmpeg = new FFmpegService();

    {
        ReflectionTestUtils.setField(ffmpeg, "ffmpegPath", "ffmpeg");
        ReflectionTestUtils.setField(ffmpeg, "ffprobePath", "ffprobe");
    }

    private File tone(double volumeDb) throws Exception {
        File out = File.createTempFile("tone_", ".mp3");
        run("ffmpeg", "-y", "-f", "lavfi", "-i", "sine=frequency=440:duration=3",
                "-af", "volume=" + volumeDb + "dB", "-ac", "1", "-acodec", "libmp3lame", out.getAbsolutePath());
        return out;
    }

    private double lufs(File file) throws Exception {
        String output = run("ffmpeg", "-hide_banner", "-nostats", "-i", file.getAbsolutePath(),
                "-af", "loudnorm=print_format=summary", "-f", "null", "-");
        Matcher matcher = Pattern.compile("Input Integrated:\\s+(-?[\\d.]+)").matcher(output);
        assertThat(matcher.find()).isTrue();
        return Double.parseDouble(matcher.group(1));
    }

    private String run(String... cmd) throws Exception {
        Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        process.waitFor();
        return out.toString();
    }

    @Test
    @DisplayName("작은 소리와 큰 소리를 모두 목표 음량(-16 LUFS) 근처로 맞춘다")
    void normalizesQuietAndLoudToTarget() throws Exception {
        File quiet = tone(-30);
        File loud = tone(-6);
        assertThat(lufs(loud) - lufs(quiet)).isGreaterThan(20);

        File quietOut = ffmpeg.normalizeLoudness(quiet);
        File loudOut = ffmpeg.normalizeLoudness(loud);

        assertThat(quietOut).isNotEqualTo(quiet);
        assertThat(lufs(quietOut)).isBetween(-17.5, -14.5);
        assertThat(lufs(loudOut)).isBetween(-17.5, -14.5);
        assertThat(Math.abs(lufs(quietOut) - lufs(loudOut))).isLessThan(1.5);
    }

    @Test
    @DisplayName("무음이거나 꺼져 있으면 원본을 그대로 돌려준다")
    void returnsOriginalForSilenceOrDisabled() throws Exception {
        File silence = ffmpeg.createSilenceFile(1000);
        assertThat(ffmpeg.normalizeLoudness(silence)).isSameAs(silence);

        File loud = tone(-6);
        ReflectionTestUtils.setField(ffmpeg, "loudnessEnabled", false);
        assertThat(ffmpeg.normalizeLoudness(loud)).isSameAs(loud);
    }
}
