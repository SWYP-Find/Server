package com.swyp.app.global.infra.media.service;

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

    public Integer getAudioDurationMs(File audioFile) throws Exception {
        String[] cmd = {
                ffprobePath, "-v", "error", "-show_entries", "format=duration",
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
        File output = File.createTempFile("silence_", ".mp3");
        double seconds = durationMs / 1000.0;

        String[] cmd = {
                ffmpegPath, "-y", "-f", "lavfi", "-i", "anullsrc=r=44100:cl=mono",
                "-t", String.valueOf(seconds), "-acodec", "libmp3lame", output.getAbsolutePath()
        };

        executeCommand(cmd);
        return output;
    }

    public File mergeAudioFiles(List<File> audioFiles) throws Exception {
        if (audioFiles == null || audioFiles.isEmpty()) throw new IllegalArgumentException("병합할 파일이 없습니다.");

        // FFmpeg에 전달할 리스트 파일 생성 (UTF-8 인코딩)
        File listFile = File.createTempFile("ffmpeg_list_", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(listFile), "UTF-8"))) {
            for (File file : audioFiles) {
                // 윈도우의 'C:\temp\file.mp3'를 'C:/temp/file.mp3'로 변환
                String safePath = file.getAbsolutePath().replace("\\", "/");
                // FFmpeg 문법에 맞춰 file '경로' 형태로 기록
                writer.write("file '" + safePath + "'");
                writer.newLine();
            }
        }

        File output = File.createTempFile("merged_full_", ".mp3");

        // 병합 명령어 구성
        String[] cmd = {
                ffmpegPath, "-y", "-f", "concat", "-safe", "0",
                "-i", listFile.getAbsolutePath(),
                "-acodec", "libmp3lame", "-q:a", "2",
                output.getAbsolutePath()
        };

        executeCommand(cmd);
        listFile.delete();
        return output;
    }

    private void executeCommand(String[] cmd) throws Exception {
        log.info("[FFmpeg] 명령어 실행: {}", String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // 에러와 출력을 합침
        Process process = pb.start();

        // 실시간 로그 출력 (에러 원인 파악용)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[FFmpeg Output] {}", line); // 문제 발생 시 로그에서 바로 확인 가능
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            log.error("[FFmpeg] 프로세스 오류 발생! Exit Code: {}", exitCode);
            throw new RuntimeException("FFmpeg 프로세스 오류 발생. Exit Code: " + exitCode);
        }
    }
}