package com.swyp.picke.domain.ticket.repository;

import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassTicketRepository extends JpaRepository<ClassTicket, Long> {

    Optional<ClassTicket> findByCodeHash(String codeHash);

    Optional<ClassTicket> findByRegisteredUserIdAndStatus(Long userId, TicketStatus status);

    /** 내 이용권 조회용: ACTIVE 가 있으면 그것, 없으면 가장 최근에 만료된 것 */
    Optional<ClassTicket> findTopByRegisteredUserIdAndStatusInOrderByActivatedAtDesc(
            Long userId, Collection<TicketStatus> statuses);
}
