package com.swyp.picke.domain.user.service.batch;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다수결 보상 배치.
 * runDate(월요일) 기준 targetDate ∈ [runDate-20, runDate-14] 범위의 배틀 중
 * 최다 득표 옵션(= 다수결 승자 옵션)을 선정하고,
 * 그 옵션을 사전 투표한 사용자 전원에게 +10P (CreditType.MAJORITY_WIN) 를 지급한다.
 *
 * referenceId = battleId. CreditHistory 유니크 제약으로 같은 배틀 재실행 시 중복 지급 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MajorityWinRewardJob {
    private final BattleRepository battleRepository;
    private final BattleOptionRepository battleOptionRepository;
    private final BattleVoteRepository battleVoteRepository;
    private final CreditService creditService;

    @Transactional
    public void run(LocalDate runDate) {
        LocalDate from = runDate.minusDays(20);
        LocalDate to = runDate.minusDays(14);
        List<Battle> battles = battleRepository
                .findByTargetDateBetweenAndStatusAndDeletedAtIsNull(from, to, BattleStatus.PUBLISHED);

        log.info("[MajorityWinRewardJob] window=[{}, {}] battles={}", from, to, battles.size());

        for (Battle battle : battles) {
            BattleOption winningOption = resolveWinningOption(battle);
            if (winningOption == null) {
                continue;
            }

            List<BattleVote> votes = battleVoteRepository.findAllByBattle(battle);
            for (BattleVote vote : votes) {
                BattleOption selected = vote.getPreVoteOption();
                if (selected == null || !selected.getId().equals(winningOption.getId())) {
                    continue;
                }
                creditService.addCredit(vote.getUser().getId(), CreditType.MAJORITY_WIN, battle.getId());
            }
        }
    }

    private BattleOption resolveWinningOption(Battle battle) {
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);
        return options.stream()
                .max(Comparator.comparingLong(o -> battleVoteRepository.countByBattleAndPreVoteOption(battle, o)))
                .orElse(null);
    }
}
