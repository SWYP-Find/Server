package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.vote.dto.request.QuizVoteRequest;
import com.swyp.picke.domain.vote.dto.response.QuizVoteResponse;

public interface QuizVoteService {
    QuizVoteResponse submitQuiz(Long battleId, Long userId, QuizVoteRequest request);
    QuizVoteResponse getMyQuizVote(Long battleId, Long userId);
    void deleteQuizVoteByBattleId(Long battleId);
}

