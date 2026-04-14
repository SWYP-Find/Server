package com.swyp.picke.domain.user.service.batch;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.service.CreditService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 베댓 보상 배치.
 * runDate(월요일) 기준 targetDate ∈ [runDate-20, runDate-14] 범위 배틀의 Perspective 중
 * 좋아요 1위(likeCount desc, createdAt desc) 작성자에게 +50P (CreditType.BEST_COMMENT).
 *
 * referenceId = perspectiveId. 재실행 멱등.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BestCommentRewardJob {

    private static final int TOP_COMMENT_LIMIT = 3;
    private static final int MIN_LIKE_COUNT = 10;

    private final BattleRepository battleRepository;
    private final PerspectiveRepository perspectiveRepository;
    private final CreditService creditService;

    @Transactional
    public void run(LocalDate runDate) {
        LocalDate from = runDate.minusDays(20);
        LocalDate to = runDate.minusDays(14);
        List<Battle> battles = battleRepository
                .findByTargetDateBetweenAndStatusAndDeletedAtIsNull(from, to, BattleStatus.PUBLISHED);

        log.info("[BestCommentRewardJob] window=[{}, {}] battles={}", from, to, battles.size());

        for (Battle battle : battles) {
            List<Perspective> topComments = perspectiveRepository
                    .findByBattleIdAndStatusOrderByLikeCountDescCreatedAtDesc(
                            battle.getId(), PerspectiveStatus.PUBLISHED, PageRequest.of(0, TOP_COMMENT_LIMIT));
            if (topComments.isEmpty()) {
                continue;
            }

            for (Perspective perspective : topComments) {
                if (perspective.getLikeCount() < MIN_LIKE_COUNT) {
                    continue;
                }
                creditService.addCredit(
                        perspective.getUser().getId(),
                        CreditType.BEST_COMMENT,
                        perspective.getId()
                );
            }
        }
    }
}
