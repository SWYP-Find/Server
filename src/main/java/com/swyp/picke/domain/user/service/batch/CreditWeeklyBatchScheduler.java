package com.swyp.picke.domain.user.service.batch;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매주 월요일 00:00 (KST) 에 크레딧 주간 배치를 실행한다.
 *
 * 다수결 보상 (+5P) — 2주 전 배틀, 총 투표수 ≥ 10 인 건의 승수 옵션 투표자 전원
 *
 * 잡의 referenceId 가 결정적(배틀ID)이므로 CreditHistory 유니크 제약으로
 * 중복 실행 시에도 추가 적립은 발생하지 않는다.
 *
 * 베댓 보상(BestCommentRewardJob, +15P)은 정책표상 "미구현 보류" 항목이라 비활성화했다.
 * 재활성화 시 runSafely("BestCommentRewardJob", () -> bestCommentRewardJob.run(runDate)) 호출을 복원하면 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditWeeklyBatchScheduler {

    private final MajorityWinRewardJob majorityWinRewardJob;

    @Scheduled(cron = "0 0 0 ? * MON", zone = "Asia/Seoul")
    public void runWeeklyBatch() {
        LocalDate runDate = LocalDate.now();
        log.info("[CreditWeeklyBatch] start runDate={}", runDate);

        runSafely("MajorityWinRewardJob", () -> majorityWinRewardJob.run(runDate));

        log.info("[CreditWeeklyBatch] end runDate={}", runDate);
    }

    private void runSafely(String name, Runnable job) {
        try {
            job.run();
        } catch (Exception e) {
            // 한 잡의 실패가 다른 잡을 막지 않도록 격리. 멱등성은 CreditHistory 유니크 제약으로 보장되므로 수동 재실행 가능.
            log.error("[CreditWeeklyBatch] {} failed", name, e);
        }
    }
}
