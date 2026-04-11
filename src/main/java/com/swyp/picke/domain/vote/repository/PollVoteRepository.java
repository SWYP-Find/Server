package com.swyp.picke.domain.vote.repository;

import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.vote.entity.PollVote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    Optional<PollVote> findByPollAndUser(Poll poll, User user);
    long countByPoll(Poll poll);
    List<PollVote> findAllByPoll(Poll poll);
}
