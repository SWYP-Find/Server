package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.vote.dto.request.VoteRequest;
import com.swyp.picke.domain.vote.dto.response.MyVoteResponse;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.dto.response.VoteStatsResponse;

/**
 * 배틀 투표. classroom 인자가 없는 메서드는 일반 배틀(class_id IS NULL),
 * classroom 을 받는 메서드는 해당 클래스 안에서의 투표만 다룬다(클래스 투표는 일반 통계·마이페이지와 섞이지 않는다).
 */
public interface BattleVoteService {
    BattleOption findPreVoteOption(Long battleId, Long userId);

    Long findPostVoteOptionId(Long battleId, Long userId);
    Long findPostVoteOptionId(Long battleId, Long userId, Classroom classroom);

    VoteStatsResponse getVoteStats(Long battleId);
    VoteStatsResponse getVoteStats(Long battleId, Classroom classroom);

    MyVoteResponse getMyVote(Long battleId, Long userId);
    MyVoteResponse getMyVote(Long battleId, Long userId, Classroom classroom);

    VoteResultResponse preVote(Long battleId, Long userId, VoteRequest request);
    VoteResultResponse preVote(Long battleId, Long userId, Classroom classroom, VoteRequest request);

    VoteResultResponse postVote(Long battleId, Long userId, VoteRequest request);
    VoteResultResponse postVote(Long battleId, Long userId, Classroom classroom, VoteRequest request);

    void deleteVotesByBattleId(Long battleId);

    void completeTts(Long battleId, Long userId);
    void completeTts(Long battleId, Long userId, Classroom classroom);
}
