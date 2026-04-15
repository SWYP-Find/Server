package com.swyp.picke.domain.poll.repository;

import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.poll.entity.PollOption;
import com.swyp.picke.domain.poll.entity.PollUserVote;
import com.swyp.picke.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollUserVoteRepository extends JpaRepository<PollUserVote, Long> {
    Optional<PollUserVote> findByPollAndUser(Poll poll, User user);
    long countByPoll(Poll poll);
    long countByPollAndSelectedOption(Poll poll, PollOption selectedOption);
    List<PollUserVote> findAllByPoll(Poll poll);
}
