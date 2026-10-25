package com.swyp.picke.domain.classroom.scheduler;

import com.swyp.picke.domain.classroom.service.retention.ClassDataPurgeService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 읽기 유예가 끝난 클래스 데이터를 하루 한 번 파기한다 (기본 매일 04:00 KST, PRD §18 "30일 이내 삭제 큐로 파기").
 * 데이터를 지우는 작업이라 picke.class-retention.purge-enabled=true 일 때만 켜진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "picke.class-retention", name = "purge-enabled", havingValue = "true")
public class ClassDataPurgeScheduler {

    static final int BATCH_SIZE = 50;
    static final int MAX_BATCHES = 20;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ClassDataPurgeService purgeService;

    @Scheduled(cron = "${picke.class-retention.purge-cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void run() {
        LocalDateTime now = LocalDateTime.now(KST);
        Set<Long> failed = new HashSet<>();
        int purged = 0;
        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            List<Long> targets = purgeService.findTargets(now, BATCH_SIZE + failed.size()).stream()
                    .filter(id -> !failed.contains(id))
                    .limit(BATCH_SIZE)
                    .toList();
            if (targets.isEmpty()) {
                break;
            }
            for (Long classId : targets) {
                try {
                    if (purgeService.purge(classId, now)) {
                        purged++;
                    }
                } catch (Exception e) {
                    failed.add(classId);
                    log.error("[ClassDataPurge] classId={} failed", classId, e);
                }
            }
        }
        log.info("[ClassDataPurge] done purged={} failed={}", purged, failed.size());
    }
}
