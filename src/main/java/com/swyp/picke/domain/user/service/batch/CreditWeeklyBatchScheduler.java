package com.swyp.picke.domain.user.service.batch;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매주 월요일 00:00 (KST) 에 크레딧 주간 배치를 실행한다.
 *
 * 세 잡을 순차로 돌린다:
 *   1) 다수결 보상 (+10P) — 2주 전 배틀, 총 투표수 ≥ 10 인 건의 승수 옵션 투표자 전원
 *   2) 베댓 보상     (+50P) — 2주 전 배틀의 Perspective 좋아요 1위 작성자
 *   3) 주간 자동 충전 (+40P) — 활성 사용자 전체
 *
 * 다수결/베댓은 동일 스냅샷 윈도우(14~20일 전 targetDate)를 공유한다.
 * 각 잡의 referenceId 가 결정적(배틀ID / perspectiveID / 주차코드)이므로
 * CreditHistory 유니크 제약으로 중복 실행 시에도 추가 적립은 발생하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditWeeklyBatchScheduler {

    private final MajorityWinRewardJob majorityWinRewardJob;
    private final BestCommentRewardJob bestCommentRewardJob;
    private final WeeklyChargeJob weeklyChargeJob;

    @Scheduled(cron = "0 0 0 ? * MON", zone = "Asia/Seoul")
    public void runWeeklyBatch() {
        LocalDate runDate = LocalDate.now();
        log.info("[CreditWeeklyBatch] start runDate={}", runDate);

        runSafely("MajorityWinRewardJob", () -> majorityWinRewardJob.run(runDate));
        runSafely("BestCommentRewardJob", () -> bestCommentRewardJob.run(runDate));
        runSafely("WeeklyChargeJob", () -> weeklyChargeJob.run(runDate));

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
