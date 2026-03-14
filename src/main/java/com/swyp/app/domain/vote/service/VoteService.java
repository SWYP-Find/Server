package com.swyp.app.domain.vote.service;

import java.util.UUID;

public interface VoteService {

    UUID findPreVoteOptionId(UUID battleId, Long userId);
}
