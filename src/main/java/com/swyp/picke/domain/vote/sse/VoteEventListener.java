package com.swyp.picke.domain.vote.sse;

import com.swyp.picke.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.picke.domain.vote.service.BattleVoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class VoteEventListener {

    private final BattleVoteService battleVoteService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVoteUpdated(VoteUpdatedEvent event) {
        VoteStatsResponse stats = battleVoteService.getVoteStats(event.battleId());
        sseEmitterRegistry.sendToAll(event.battleId(), stats);
    }
}
