package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.vote.dto.request.QuizVoteRequest;
import com.swyp.picke.domain.vote.dto.response.PollVoteResponse;
import com.swyp.picke.domain.vote.dto.response.QuizVoteResponse;

public interface QuizVoteService {
    QuizVoteResponse submitQuiz(Long battleId, Long userId, QuizVoteRequest request);
    PollVoteResponse submitPoll(Long battleId, Long userId, QuizVoteRequest request);
    QuizVoteResponse getMyQuizVote(Long battleId, Long userId);
    PollVoteResponse getMyPollVote(Long battleId, Long userId);
}
