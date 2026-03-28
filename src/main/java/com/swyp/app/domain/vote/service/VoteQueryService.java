package com.swyp.app.domain.vote.service;

import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import com.swyp.app.domain.vote.entity.Vote;
import com.swyp.app.domain.vote.enums.VoteStatus;
import com.swyp.app.domain.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteQueryService {

    private final VoteRepository voteRepository;

    public List<Vote> findUserVotes(Long userId, int offset, int size, BattleOptionLabel label) {
        PageRequest pageable = PageRequest.of(offset / size, size);
        return label != null
                ? voteRepository.findByUserIdAndPreVoteOptionLabelOrderByCreatedAtDesc(userId, label, pageable)
                : voteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long countUserVotes(Long userId, BattleOptionLabel label) {
        return label != null
                ? voteRepository.countByUserIdAndPreVoteOptionLabel(userId, label)
                : voteRepository.countByUserId(userId);
    }

    public long countTotalParticipation(Long userId) {
        return voteRepository.countByUserId(userId);
    }

    public long countOpinionChanges(Long userId) {
        return voteRepository.countOpinionChangesByUserId(userId);
    }

    public int calculateBattleWinRate(Long userId) {
        List<Vote> postVotes = voteRepository.findByUserId(userId).stream()
                .filter(v -> v.getStatus() == VoteStatus.POST_VOTED && v.getPostVoteOption() != null)
                .toList();

        if (postVotes.isEmpty()) return 0;

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
        return voteRepository.findByUserId(userId).stream()
                .map(v -> v.getBattle().getId())
                .distinct()
                .toList();
    }

    public List<Long> findFirstNBattleIds(Long userId, int n) {
        return voteRepository.findByUserIdOrderByCreatedAtAsc(userId, PageRequest.of(0, n)).stream()
                .map(v -> v.getBattle().getId())
                .distinct()
                .toList();
    }
}
