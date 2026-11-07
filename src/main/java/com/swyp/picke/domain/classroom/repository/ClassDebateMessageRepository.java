package com.swyp.picke.domain.classroom.repository;

import com.swyp.picke.domain.classroom.entity.ClassDebateMessage;
import com.swyp.picke.domain.classroom.enums.ClassDebateSpeaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassDebateMessageRepository extends JpaRepository<ClassDebateMessage, Long> {

    List<ClassDebateMessage> findBySessionIdOrderByRoundIndexAscIdAsc(Long sessionId);

    Optional<ClassDebateMessage> findBySessionIdAndRoundIndexAndSpeaker(Long sessionId, int roundIndex, ClassDebateSpeaker speaker);
}
