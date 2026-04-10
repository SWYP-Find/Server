package com.swyp.picke.domain.poll.repository;

import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.poll.entity.PollTagMap;
import com.swyp.picke.domain.poll.entity.PollTagMapId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollTagMapRepository extends JpaRepository<PollTagMap, PollTagMapId> {
    List<PollTagMap> findByPoll(Poll poll);
    void deleteByPoll(Poll poll);
}

