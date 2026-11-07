package com.swyp.picke.domain.ticket.repository;

import com.swyp.picke.domain.ticket.entity.TicketRegisterReminder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRegisterReminderRepository extends JpaRepository<TicketRegisterReminder, Long> {

    List<TicketRegisterReminder> findByTicketIdOrderByAttemptedAtDesc(Long ticketId);
}
