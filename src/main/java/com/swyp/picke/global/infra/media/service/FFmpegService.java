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
