package com.swyp.picke.domain.battle.repository.projection;

public interface BattleParticipationStats {
    long getBattleCount();
    Double getAvgPreVoteRate();
    Double getAvgPostVoteRate();
    Double getAvgPerspectiveRate();
    Double getAvgCommentRate();
}
