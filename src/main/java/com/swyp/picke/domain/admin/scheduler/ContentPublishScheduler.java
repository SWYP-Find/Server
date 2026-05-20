package com.swyp.picke.domain.admin.scheduler;

import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.poll.service.PollService;
import com.swyp.picke.domain.quiz.service.QuizService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentPublishScheduler {

    private static final String SEOUL_ZONE_ID = "Asia/Seoul";
    private static final ZoneId SEOUL_ZONE = ZoneId.of(SEOUL_ZONE_ID);

    private final BattleService battleService;
    private final QuizService quizService;
    private final PollService pollService;

    @Scheduled(cron = "0 0 0 * * *", zone = SEOUL_ZONE_ID)
    public void openReadyBattles() {
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        int openedCount = battleService.openReadyBattles(now);
        log.info("배틀 예약 발행 스케줄러 완료. 공개건수={}, 실행시각={}", openedCount, now);
    }

    @Scheduled(cron = "0 0 0 * * *", zone = SEOUL_ZONE_ID)
    public void openReadyQuizzesAndPolls() {
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        int quizOpenedCount = quizService.openReadyQuizzes(now);
        int pollOpenedCount = pollService.openReadyPolls(now);
        log.info("퀴즈/투표 예약 발행 스케줄러 완료. 퀴즈공개건수={}, 투표공개건수={}, 실행시각={}",
                quizOpenedCount,
                pollOpenedCount,
                now);
    }
}
