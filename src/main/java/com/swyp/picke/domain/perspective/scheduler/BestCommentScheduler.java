package com.swyp.picke.domain.perspective.scheduler;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BestCommentScheduler {

    private static final int MIN_LIKE_COUNT = 10;
    private static final int TOP_N = 3;

    private final BattleRepository battleRepository;
    private final PerspectiveCommentRepository perspectiveCommentRepository;
    private final CreditService creditService;

    @Scheduled(cron = "0 0 0 * * MON")
    public void awardBestComments() {
        log.info("[BestCommentScheduler] 베스트 댓글 포인트 정산 시작");

        List<Battle> battles = battleRepository.findByStatusAndDeletedAtIsNull(BattleStatus.PUBLISHED);

        for (Battle battle : battles) {
            try {
                processBattle(battle.getId());
            } catch (Exception e) {
                log.error("[BestCommentScheduler] battleId={} 처리 중 오류 발생: {}", battle.getId(), e.getMessage());
            }
        }

        log.info("[BestCommentScheduler] 베스트 댓글 포인트 정산 완료");
    }

    @Transactional
    public void processBattle(Long battleId) {
        List<PerspectiveComment> topComments = perspectiveCommentRepository.findTopCommentsByBattleId(
                battleId,
                MIN_LIKE_COUNT,
                PageRequest.of(0, TOP_N)
        );

        if (topComments.isEmpty()) {
            return;
        }

        for (PerspectiveComment comment : topComments) {
            Long userId = comment.getUser().getId();
            Long commentId = comment.getId();

            creditService.addCredit(userId, CreditType.BEST_COMMENT, commentId);
            log.info("[BestCommentScheduler] 포인트 지급 - battleId={}, commentId={}, userId={}", battleId, commentId, userId);
        }
    }
}
