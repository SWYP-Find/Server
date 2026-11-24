package com.swyp.picke.global.infra.media.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;

@Slf4j
@Component
public class FFmpegService implements AudioProcessor {

    @Value("${media.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${media.ffprobe.path:ffprobe}")
    private String ffprobePath;

    /** 대사 음성을 이어 붙이기 전에 같은 음량으로 맞출지 (#701) */
    @Value("${media.loudness.enabled:true}")
    private boolean loudnessEnabled = true;

    /** 목표 통합 음량(LUFS). 모바일 음성 콘텐츠 기준 -16 */
    @Value("${media.loudness.target-lufs:-16}")
    private double targetLufs = -16;

    /** 목표 최대 피크(dBTP). 이어 붙이고 mp3 로 다시 인코딩할 때 클리핑 여유 */
    @Value("${media.loudness.true-peak:-1.5}")
    private double truePeak = -1.5;

    private static final java.util.regex.Pattern LOUDNORM_JSON = java.util.regex.Pattern.compile("\\{[^{}]*\"input_i\"[^{}]*\\}");

    private String resolveExecutable(String configuredPath, String fallback) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return resolveFallbackExecutable(fallback);
        }

        File candidate = new File(configuredPath);
        if (isAbsolutePath(configuredPath) && !candidate.exists()) {
            log.warn("[FFmpeg] 설정된 실행 파일을 찾지 못해 PATH 명령어로 대체합니다: {}", configuredPath);
            return resolveFallbackExecutable(fallback);
        }

        return configuredPath;
    }

    private boolean isAbsolutePath(String path) {
        return new File(path).isAbsolute() || path.startsWith("/") || path.startsWith("\\");
    }

    private String resolveFallbackExecutable(String fallback) {
        if (File.separatorChar != '\\') {
            return fallback;
        }

        try {
            Process process = new ProcessBuilder("where.exe", fallback).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String resolved = reader.readLine();
                process.waitFor();
                if (resolved != null && !resolved.isBlank()) {
                    return resolved.trim();
                }
            }
        } catch (Exception e) {
            log.debug("[FFmpeg] 대체 실행 파일 탐색 실패: {}, 원인={}", fallback, e.getMessage());
        }

        return fallback;
    }

    public Integer getAudioDurationMs(File audioFile) throws Exception {
        String resolvedFfprobePath = resolveExecutable(ffprobePath, "ffprobe");
        String[] cmd = {
                resolvedFfprobePath, "-v", "error", "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", audioFile.getAbsolutePath()
        };
        Process process = new ProcessBuilder(cmd).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            process.waitFor();
            return (line != null) ? (int) (Double.parseDouble(line) * 1000) : 0;
        }
    }

    public File createSilenceFile(int durationMs) throws Exception {
        String resolvedFfmpegPath = resolveExecutable(ffmpegPath, "ffmpeg");
        File output = File.createTempFile("silence_", ".mp3");
        double seconds = durationMs / 1000.0;

        String[] cmd = {
                resolvedFfmpegPath, "-y", "-f", "lavfi", "-i", "anullsrc=r=44100:cl=mono",
                "-t", String.valueOf(seconds), "-acodec", "libmp3lame", output.getAbsolutePath()
        };

        executeCommand(cmd);
        return output;
    }

    public File mergeAudioFiles(List<File> audioFiles) throws Exception {
        if (audioFiles == null || audioFiles.isEmpty()) throw new IllegalArgumentException("merged audio file list is empty.");
        String resolvedFfmpegPath = resolveExecutable(ffmpegPath, "ffmpeg");

        File listFile = File.createTempFile("ffmpeg_list_", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(listFile), "UTF-8"))) {
            for (File file : audioFiles) {
                String safePath = file.getAbsolutePath().replace("\\", "/");
                writer.write("file '" + safePath + "'");
                writer.newLine();
            }
        }

        File output = File.createTempFile("merged_full_", ".mp3");

        String[] cmd = {
                resolvedFfmpegPath, "-y", "-f", "concat", "-safe", "0",
                "-i", listFile.getAbsolutePath(),
                "-acodec", "libmp3lame", "-q:a", "2",
                output.getAbsolutePath()
        };

        executeCommand(cmd);
        listFile.delete();
        return output;
    }

    /**
     * 음량을 목표치로 맞춘 새 mp3 를 돌려준다 (#701). ffmpeg loudnorm 2-pass: 먼저 재고, 잰 값으로 linear 하게 맞춘다.
     * linear 라 말소리 강약은 그대로 두고 전체 크기만 옮긴다.
     * 꺼져 있거나, 무음이거나, 측정·변환에 실패하면 원본을 그대로 돌려준다 (음성 생성 자체를 막지 않음).
     */
    public File normalizeLoudness(File input) {
        if (!loudnessEnabled || input == null || !input.exists()) {
            return input;
        }
        String resolvedFfmpegPath = resolveExecutable(ffmpegPath, "ffmpeg");
        String target = "I=" + targetLufs + ":TP=" + truePeak + ":LRA=11";
        try {
            String measured = runAndCapture(new String[]{
                    resolvedFfmpegPath, "-hide_banner", "-nostats", "-i", input.getAbsolutePath(),
                    "-af", "loudnorm=" + target + ":print_format=json", "-f", "null", "-"
            });
            java.util.regex.Matcher matcher = LOUDNORM_JSON.matcher(measured);
            String json = null;
            while (matcher.find()) {
                json = matcher.group();
            }
            if (json == null) {
                log.warn("[FFmpeg] 음량 측정 결과 없음 - 원본 사용: {}", input.getName());
                return input;
            }
            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            String inputI = node.path("input_i").asText("");
            if (inputI.isBlank() || inputI.contains("inf")) {
                return input; // 무음 구간은 맞출 대상이 없다
            }
            String filter = "loudnorm=" + target
                    + ":measured_I=" + inputI
                    + ":measured_TP=" + node.path("input_tp").asText()
                    + ":measured_LRA=" + node.path("input_lra").asText()
                    + ":measured_thresh=" + node.path("input_thresh").asText()
                    + ":offset=" + node.path("target_offset").asText()
                    + ":linear=true,aresample=44100";
            File output = File.createTempFile("loudnorm_", ".mp3");
            executeCommand(new String[]{
                    resolvedFfmpegPath, "-y", "-i", input.getAbsolutePath(), "-af", filter,
                    "-ac", "1", "-acodec", "libmp3lame", "-q:a", "2", output.getAbsolutePath()
            });
            log.info("[FFmpeg] 음량 맞춤 {} LUFS -> {} LUFS: {}", inputI, targetLufs, input.getName());
            return output;
        } catch (Exception e) {
            log.warn("[FFmpeg] 음량 맞춤 실패 - 원본 사용: {} ({})", input.getName(), e.getMessage());
            return input;
        }
    }

    /** 출력(표준에러 포함)을 문자열로 돌려준다. 측정용이라 줄마다 로그를 남기지 않는다 */
    private String runAndCapture(String[] cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg 측정 오류. Exit Code: " + exitCode);
        }
        return out.toString();
    }

    private void executeCommand(String[] cmd) throws Exception {
        log.info("[FFmpeg] 명령 실행: {}", String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[FFmpeg 출력] {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("[FFmpeg] 프로세스 오류 발생. Exit Code: {}", exitCode);
            throw new RuntimeException("FFmpeg 프로세스 오류 발생. Exit Code: " + exitCode);
        }
    }
}
