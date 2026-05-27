package com.swyp.picke.domain.battle.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.perspective.service.GptPerspectiveGenerationService;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserBattleService;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class BattleAutoSeedService {

    private final UserRepository userRepository;
    private final BattleVoteRepository battleVoteRepository;
    private final UserBattleService userBattleService;
    private final CreditService creditService;
    private final PerspectiveRepository perspectiveRepository;
    private final GptPerspectiveGenerationService gptPerspectiveGenerationService;

    @Transactional
    public void seed(Battle battle, List<BattleOption> options, int botCount) {
        if (options.size() < 2) {
            log.warn("옵션이 2개 미만이라 관점 자동 생성을 건너뜁니다. battleId={}", battle.getId());
            return;
        }

        List<User> bots = userRepository.findAllByRole(UserRole.BOT);
        if (bots.isEmpty()) {
            log.warn("BOT 계정이 없어 관점 자동 생성을 건너뜁니다. battleId={}", battle.getId());
            return;
        }

        List<User> selectedBots = new ArrayList<>(bots);
        Collections.shuffle(selectedBots);
        int n = Math.min(botCount, selectedBots.size());
        selectedBots = selectedBots.subList(0, n);

        BattleOption optionA = options.get(0);
        BattleOption optionB = options.get(1);

        int half = n / 2;
        int optionACount = (n % 2 == 0) ? half : (ThreadLocalRandom.current().nextBoolean() ? half : half + 1);

        for (int i = 0; i < selectedBots.size(); i++) {
            User bot = selectedBots.get(i);
            BattleOption postOption = i < optionACount ? optionA : optionB;
            BattleOption otherOption = postOption == optionA ? optionB : optionA;

            // preVoteOption: 50% 확률로 같거나 다른 옵션
            BattleOption preOption = ThreadLocalRandom.current().nextBoolean() ? postOption : otherOption;

            try {
                BattleVote vote = BattleVote.builder()
                        .user(bot)
                        .battle(battle)
                        .preVoteOption(preOption)
                        .postVoteOption(postOption)
                        .isTtsListened(false)
                        .build();
                battleVoteRepository.save(vote);

                creditService.addCredit(bot.getId(), CreditType.BATTLE_VOTE, vote.getId());

                userBattleService.upsertStep(bot, battle, UserBattleStep.COMPLETED);

                battle.addParticipant();

                String content = gptPerspectiveGenerationService.generate(battle, postOption);

                Perspective perspective = Perspective.builder()
                        .battle(battle)
                        .user(bot)
                        .option(postOption)
                        .content(content)
                        .build();
                perspective.publish();
                perspectiveRepository.save(perspective);

            } catch (Exception e) {
                log.error("봇 관점 자동 생성 실패. botId={}, battleId={}", bot.getId(), battle.getId(), e);
                throw new RuntimeException("봇 관점 자동 생성 실패로 전체 롤백. botId=" + bot.getId(), e);
            }
        }
    }
}
