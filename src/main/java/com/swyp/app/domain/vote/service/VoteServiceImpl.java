package com.swyp.app.domain.vote.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VoteServiceImpl implements VoteService {

    @Override
    public UUID findPreVoteOptionId(UUID battleId, Long userId) {
        throw new UnsupportedOperationException("Not yet implemented - pending Vote domain merge");
    }
}
