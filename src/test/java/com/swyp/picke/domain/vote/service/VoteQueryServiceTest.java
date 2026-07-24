package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.user.enums.VoteSide;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoteQueryServiceTest {

    @Mock
    private BattleVoteRepository battleVoteRepository;

    @InjectMocks
    private VoteQueryService voteQueryService;

    @Test
    @DisplayName("voteSide 없이 조회하면 사후투표를 완료한 기록만 조회한다")
    void findUserVotes_without_voteSide_uses_post_vote_completed_query() {
        when(battleVoteRepository.findByUserIdAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(List.of());

        voteQueryService.findUserVotes(1L, 0, 20, null);

        verify(battleVoteRepository).findByUserIdAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(eq(1L), any());
    }

    @Test
    @DisplayName("voteSide=PRO로 조회하면 사후투표를 완료한 PRO 기록만 조회한다")
    void findUserVotes_with_pro_uses_post_vote_completed_query() {
        when(battleVoteRepository.findByUserIdAndPreVoteOptionDisplayOrderAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(eq(1L), eq(1), any()))
                .thenReturn(List.of());

        voteQueryService.findUserVotes(1L, 0, 20, VoteSide.PRO);

        verify(battleVoteRepository).findByUserIdAndPreVoteOptionDisplayOrderAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(eq(1L), eq(1), any());
    }

    @Test
    @DisplayName("voteSide=CON으로 조회하면 사후투표를 완료한 CON 기록만 조회한다")
    void findUserVotes_with_con_uses_post_vote_completed_query() {
        when(battleVoteRepository.findByUserIdAndPreVoteOptionDisplayOrderNotAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(eq(1L), eq(1), any()))
                .thenReturn(List.of());

        voteQueryService.findUserVotes(1L, 0, 20, VoteSide.CON);

        verify(battleVoteRepository).findByUserIdAndPreVoteOptionDisplayOrderNotAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(eq(1L), eq(1), any());
    }

    @Test
    @DisplayName("voteSide 없이 카운트하면 사후투표를 완료한 기록만 센다")
    void countUserVotes_without_voteSide_uses_post_vote_completed_query() {
        when(battleVoteRepository.countByUserIdAndPostVoteOptionIsNotNull(1L)).thenReturn(3L);

        long count = voteQueryService.countUserVotes(1L, null);

        assertThat(count).isEqualTo(3L);
        verify(battleVoteRepository).countByUserIdAndPostVoteOptionIsNotNull(1L);
    }

    @Test
    @DisplayName("총 참여 횟수(countTotalParticipation)는 사전투표만 한 기록도 포함한다")
    void countTotalParticipation_includes_pre_vote_only_records() {
        when(battleVoteRepository.countByUserId(1L)).thenReturn(5L);

        long count = voteQueryService.countTotalParticipation(1L);

        assertThat(count).isEqualTo(5L);
        verify(battleVoteRepository).countByUserId(1L);
    }
}
