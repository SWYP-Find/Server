package com.swyp.picke.domain.classroom.scheduler;

import com.swyp.picke.domain.classroom.service.report.ClassReportGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 클래스 마감 감지와 AI 리포트 생성을 주기적으로 돌린다 (기본 60초, picke.class-report.scheduler-delay-ms).
 * fixedDelay 라 이전 실행이 끝난 뒤부터 간격을 재므로 같은 리포트를 동시에 두 번 처리하지 않는다.
 * 테스트 프로필은 scheduler-enabled=false 로 끈다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "picke.class-report", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ClassReportScheduler {

    private final ClassReportGenerationService generationService;

    @Scheduled(fixedDelayString = "${picke.class-report.scheduler-delay-ms:60000}", initialDelayString = "30000")
    public void run() {
        try {
            generationService.runOnce();
        } catch (Exception e) {
            log.error("[ClassReportScheduler] tick failed", e);
        }
    }
}
