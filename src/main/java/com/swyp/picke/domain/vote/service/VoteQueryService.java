package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.user.enums.VoteSide;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteQueryService {

    private final BattleVoteRepository battleVoteRepository;

    public List<BattleVote> findUserVotes(Long userId, int offset, int size, VoteSide voteSide) {
        PageRequest pageable = PageRequest.of(offset / size, size);
        if (voteSide == VoteSide.PRO) {
            return battleVoteRepository.findByUserIdAndPreVoteOptionDisplayOrderAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(userId, 1, pageable);
        }
        if (voteSide == VoteSide.CON) {
            return battleVoteRepository.findByUserIdAndPreVoteOptionDisplayOrderNotAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(userId, 1, pageable);
        }
        return battleVoteRepository.findByUserIdAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(userId, pageable);
    }

    public long countUserVotes(Long userId, VoteSide voteSide) {
        if (voteSide == VoteSide.PRO) {
            return battleVoteRepository.countByUserIdAndPreVoteOptionDisplayOrderAndPostVoteOptionIsNotNull(userId, 1);
        }
        if (voteSide == VoteSide.CON) {
            return battleVoteRepository.countByUserIdAndPreVoteOptionDisplayOrderNotAndPostVoteOptionIsNotNull(userId, 1);
        }
        return battleVoteRepository.countByUserIdAndPostVoteOptionIsNotNull(userId);
    }

    public long countTotalParticipation(Long userId) {
        return battleVoteRepository.countByUserId(userId);
    }

    public long countOpinionChanges(Long userId) {
        return battleVoteRepository.countOpinionChangesByUserId(userId);
    }

    public int calculateBattleWinRate(Long userId) {
        List<BattleVote> postVotes = battleVoteRepository.findByUserId(userId).stream()
                .filter(v -> v.getPostVoteOption() != null)
                .toList();

        if (postVotes.isEmpty()) {
            return 0;
        }

        long wins = postVotes.stream()
                .filter(v -> {
                    BattleOption myOption = v.getPostVoteOption();
                    BattleOption otherOption = v.getPreVoteOption();
                    if (myOption.getId().equals(otherOption.getId())) {
                        long totalVotes = v.getBattle().getTotalParticipantsCount();
                        return myOption.getVoteCount() > totalVotes - myOption.getVoteCount();
                    }
                    return myOption.getVoteCount() > otherOption.getVoteCount();
                })
                .count();

        return (int) (wins * 100 / postVotes.size());
    }

    public List<Long> findParticipatedBattleIds(Long userId) {
        return battleVoteRepository.findByUserId(userId).stream()
                .map(v -> v.getBattle().getId())
                .distinct()
                .toList();
    }

    public List<Long> findFirstNBattleIds(Long userId, int n) {
        return battleVoteRepository.findByUserIdOrderByCreatedAtAsc(userId, PageRequest.of(0, n)).stream()
                .map(v -> v.getBattle().getId())
                .distinct()
                .toList();
    }

    public List<Long> findFirstNVotedOptionIds(Long userId, int n) {
        return battleVoteRepository.findByUserIdOrderByCreatedAtAsc(userId, PageRequest.of(0, n)).stream()
                .map(v -> {
                    if (v.getPostVoteOption() != null) {
                        return v.getPostVoteOption().getId();
                    }
                    if (v.getPreVoteOption() != null) {
                        return v.getPreVoteOption().getId();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
