package com.swyp.app.global.infra.media.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;

/**
 * FFmpeg를 활용한 실제 오디오 처리 서비스 (메인 사용)
 * - 외부 프로세스인 ffprobe와 ffmpeg 명령어를 호출하여 파일 길이 측정 및 파일 병합 수행
 */
@Slf4j
@Component
public class FFmpegService implements AudioProcessor {

    /**
     * 오디오 파일의 길이를 밀리초(ms) 단위로 반환합니다.
     */
    public Integer getAudioDurationMs(File audioFile) throws Exception {
        String[] cmd = {
                "ffprobe", "-v", "error", "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", audioFile.getAbsolutePath()
        };
        Process process = new ProcessBuilder(cmd).start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        process.waitFor();
        return (line != null) ? (int) (Double.parseDouble(line) * 1000) : 0;
    }

    /**
     * 지정된 밀리초(ms)만큼의 무음 MP3 파일을 생성
     * 연기 톤 개선을 위한 '숨 고르기' 구간용
     */
    public File createSilenceFile(int durationMs) throws Exception {
        File output = File.createTempFile("silence_", ".mp3");
        double seconds = durationMs / 1000.0;

        String[] cmd = {
                "ffmpeg", "-y", "-f", "lavfi", "-i", "anullsrc=r=44100:cl=mono",
                "-t", String.valueOf(seconds), "-acodec", "libmp3lame", output.getAbsolutePath()
        };

        executeCommand(cmd);
        return output;
    }

    /**
     * 여러 MP3 파일을 하나로 병합 (Re-encoding 방식 권장)
     */
    public File mergeAudioFiles(List<File> audioFiles) throws Exception {
        if (audioFiles == null || audioFiles.isEmpty()) throw new IllegalArgumentException("병합할 파일이 없습니다.");

        File listFile = File.createTempFile("ffmpeg_list_", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(listFile))) {
            for (File file : audioFiles) {
                writer.write("file '" + file.getAbsolutePath().replace("\\", "/") + "'\n");
            }
        }

        File output = File.createTempFile("merged_full_", ".mp3");

        String[] cmd = {
                "ffmpeg", "-y", "-f", "concat", "-safe", "0",
                "-i", listFile.getAbsolutePath(), "-acodec", "libmp3lame", "-q:a", "2", output.getAbsolutePath()
        };

        executeCommand(cmd);
        listFile.delete();
        return output;
    }

    private void executeCommand(String[] cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // 에러 스트림을 일반 스트림으로 합침
        Process process = pb.start();

        // FFmpeg이 쏟아내는 출력물을 계속 읽어서 버퍼를 비워줍니다.
        // 이걸 안 해주면 출력 버퍼가 꽉 차서 process.waitFor()가 영원히 무한 대기(Deadlock)에 빠짐
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 로그가 너무 길어지면 지저분하므로 무시 (디버깅이 필요할 때만 log.debug 사용)
            }
        }

        int exitCode = process.waitFor(); // 버퍼가 비워졌으므로 정상적으로 종료 감지

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg 프로세스 오류 발생. Exit Code: " + exitCode);
        }
    }
}