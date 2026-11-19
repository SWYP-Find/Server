package com.swyp.picke.domain.ticket.repository;

import com.swyp.picke.domain.ticket.entity.TicketCodeAttempt;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketCodeAttemptRepository extends JpaRepository<TicketCodeAttempt, Long> {

    /** 동시에 여러 번 틀려도 횟수가 빠지지 않도록 행을 잠그고 읽는다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from TicketCodeAttempt a where a.userId = :userId")
    Optional<TicketCodeAttempt> findByUserIdForUpdate(@Param("userId") Long userId);
}
