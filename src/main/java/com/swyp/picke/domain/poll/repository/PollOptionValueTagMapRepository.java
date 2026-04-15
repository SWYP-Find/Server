package com.swyp.picke.domain.poll.repository;

import com.swyp.picke.domain.poll.entity.PollOption;
import com.swyp.picke.domain.poll.entity.PollOptionValueTagMap;
import com.swyp.picke.domain.poll.entity.PollOptionValueTagMapId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionValueTagMapRepository extends JpaRepository<PollOptionValueTagMap, PollOptionValueTagMapId> {
    List<PollOptionValueTagMap> findByPollOption(PollOption pollOption);
    void deleteByPollOption(PollOption pollOption);
}

