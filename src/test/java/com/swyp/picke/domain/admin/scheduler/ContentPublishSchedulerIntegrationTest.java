package com.swyp.picke.domain.admin.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.poll.enums.PollStatus;
import com.swyp.picke.domain.poll.repository.PollRepository;
import com.swyp.picke.domain.quiz.entity.Quiz;
import com.swyp.picke.domain.quiz.enums.QuizStatus;
import com.swyp.picke.domain.quiz.repository.QuizRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContentPublishSchedulerIntegrationTest {

    @Autowired
    private ContentPublishScheduler contentPublishScheduler;

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private S3Client s3Client;

    @Test
    @DisplayName("예약 시간이 지난 콘텐츠만 자동 공개된다")
    void publishReadyContents_whenPublishAtHasPassed() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDate targetDate = now.toLocalDate();

        Battle dueBattle = battleRepository.save(Battle.builder()
                .title("예약 배틀")
                .summary("예약 배틀 요약")
                .description("예약 배틀 설명")
                .targetDate(targetDate)
                .publishAt(now.minusMinutes(1))
                .status(BattleStatus.PENDING)
                .creatorType(BattleCreatorType.ADMIN)
                .build());
        Battle futureBattle = battleRepository.save(Battle.builder()
                .title("미래 배틀")
                .targetDate(targetDate)
                .publishAt(now.plusDays(1))
                .status(BattleStatus.PENDING)
                .creatorType(BattleCreatorType.ADMIN)
                .build());

        Quiz dueQuiz = quizRepository.save(Quiz.builder()
                .title("예약 퀴즈")
                .targetDate(targetDate)
                .publishAt(now.minusMinutes(1))
                .status(QuizStatus.PENDING)
                .build());
        Quiz futureQuiz = quizRepository.save(Quiz.builder()
                .title("미래 퀴즈")
                .targetDate(targetDate)
                .publishAt(now.plusDays(1))
                .status(QuizStatus.PENDING)
                .build());

        Poll duePoll = pollRepository.save(Poll.builder()
                .titlePrefix("예약 투표")
                .titleSuffix("선택")
                .targetDate(targetDate)
                .publishAt(now.minusMinutes(1))
                .status(PollStatus.PENDING)
                .build());
        Poll futurePoll = pollRepository.save(Poll.builder()
                .titlePrefix("미래 투표")
                .titleSuffix("선택")
                .targetDate(targetDate)
                .publishAt(now.plusDays(1))
                .status(PollStatus.PENDING)
                .build());

        entityManager.flush();
        entityManager.clear();

        contentPublishScheduler.openReadyBattles();
        contentPublishScheduler.openReadyQuizzesAndPolls();

        entityManager.flush();
        entityManager.clear();

        assertThat(battleRepository.findById(dueBattle.getId()).orElseThrow().getStatus())
                .isEqualTo(BattleStatus.PUBLISHED);
        assertThat(battleRepository.findById(futureBattle.getId()).orElseThrow().getStatus())
                .isEqualTo(BattleStatus.PENDING);
        assertThat(quizRepository.findById(dueQuiz.getId()).orElseThrow().getStatus())
                .isEqualTo(QuizStatus.PUBLISHED);
        assertThat(quizRepository.findById(futureQuiz.getId()).orElseThrow().getStatus())
                .isEqualTo(QuizStatus.PENDING);
        assertThat(pollRepository.findById(duePoll.getId()).orElseThrow().getStatus())
                .isEqualTo(PollStatus.PUBLISHED);
        assertThat(pollRepository.findById(futurePoll.getId()).orElseThrow().getStatus())
                .isEqualTo(PollStatus.PENDING);
    }
}
