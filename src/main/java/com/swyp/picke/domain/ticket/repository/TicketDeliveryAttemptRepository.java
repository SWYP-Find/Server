package com.swyp.picke.domain.ticket.repository;

import com.swyp.picke.domain.ticket.entity.TicketDeliveryAttempt;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketDeliveryAttemptRepository extends JpaRepository<TicketDeliveryAttempt, Long> {

    List<TicketDeliveryAttempt> findByTicketIdOrderByAttemptedAtDesc(Long ticketId);

    List<TicketDeliveryAttempt> findByTicketIdInOrderByIdDesc(Collection<Long> ticketIds);
}
