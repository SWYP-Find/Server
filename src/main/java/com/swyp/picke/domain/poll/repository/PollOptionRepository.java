package com.swyp.picke.domain.poll.repository;

import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.poll.entity.PollOption;
import com.swyp.picke.domain.poll.enums.PollOptionLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
    List<PollOption> findByPollOrderByDisplayOrderAscLabelAscIdAsc(Poll poll);
    Optional<PollOption> findByPollAndLabel(Poll poll, PollOptionLabel label);
}
