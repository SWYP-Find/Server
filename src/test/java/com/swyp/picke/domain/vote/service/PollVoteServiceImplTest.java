package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.poll.entity.PollOption;
import com.swyp.picke.domain.poll.enums.PollOptionLabel;
import com.swyp.picke.domain.poll.enums.PollStatus;
import com.swyp.picke.domain.poll.repository.PollOptionRepository;
import com.swyp.picke.domain.poll.service.PollService;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.vote.dto.request.PollVoteRequest;
import com.swyp.picke.domain.vote.dto.response.PollVoteResponse;
import com.swyp.picke.domain.vote.entity.PollVote;
import com.swyp.picke.domain.vote.repository.PollVoteRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollVoteServiceImplTest {

    @Mock
    private PollService pollService;

    @Mock
    private PollOptionRepository pollOptionRepository;

    @Mock
    private PollVoteRepository pollVoteRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PollVoteServiceImpl pollVoteService;

    @Test
    @DisplayName("폴 신규 투표 시 totalParticipantsCount가 증가한다")
    void submitPoll_increases_totalParticipants_on_new_vote() {
        Long pollId = 1L;
        Long userId = 10L;
        Long optionId = 201L;

        Poll poll = Poll.builder()
                .titlePrefix("찬성")
                .titleSuffix("반대")
                .targetDate(LocalDate.now())
                .status(PollStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(poll, "id", pollId);

        PollOption optionA = PollOption.builder()
                .poll(poll)
                .label(PollOptionLabel.A)
                .title("찬성")
                .displayOrder(1)
                .voteCount(0L)
                .build();
        ReflectionTestUtils.setField(optionA, "id", optionId);

        User user = org.mockito.Mockito.mock(User.class);

        when(pollService.findById(pollId)).thenReturn(poll);
        when(pollOptionRepository.findById(optionId)).thenReturn(Optional.of(optionA));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(pollVoteRepository.findByPollAndUser(poll, user)).thenReturn(Optional.empty());
        when(pollVoteRepository.save(any(PollVote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pollOptionRepository.findByPollOrderByDisplayOrderAscLabelAscIdAsc(poll)).thenReturn(List.of(optionA));

        PollVoteResponse response = pollVoteService.submitPoll(pollId, userId, new PollVoteRequest(optionId));

        assertThat(poll.getTotalParticipantsCount()).isEqualTo(1L);
        assertThat(optionA.getVoteCount()).isEqualTo(1L);
        assertThat(response.totalCount()).isEqualTo(1L);
        assertThat(response.selectedOptionId()).isEqualTo(optionId);
    }
}