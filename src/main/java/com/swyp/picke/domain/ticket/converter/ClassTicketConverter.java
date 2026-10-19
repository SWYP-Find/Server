package com.swyp.picke.domain.ticket.converter;

import com.swyp.picke.domain.ticket.dto.response.ClassTicketPreviewResponse;
import com.swyp.picke.domain.ticket.dto.response.ClassTicketResponse;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ClassTicketConverter {

    public static ClassTicketResponse toResponse(ClassTicket ticket, LocalDateTime now) {
        // 날짜 단위 D-day. 등록 직후 하루 동안 값이 흔들리지 않도록 시각이 아니라 날짜로 뺀다
        long remainingDays = 0;
        if (ticket.getExpiresAt() != null && now.isBefore(ticket.getExpiresAt())) {
            remainingDays = ChronoUnit.DAYS.between(now.toLocalDate(), ticket.getExpiresAt().toLocalDate());
        }
        return new ClassTicketResponse(
                ticket.getId(),
                ticket.getCapacity(),
                ticket.getStatus(),
                ticket.getActivatedAt(),
                ticket.getExpiresAt(),
                remainingDays,
                ticket.getReadGraceEndsAt()
        );
    }

    public static ClassTicketPreviewResponse toPreview(ClassTicket ticket, boolean registeredByMe) {
        return new ClassTicketPreviewResponse(
                ticket.getCapacity(),
                ticket.getRegisterBy(),
                ClassTicket.USAGE_MONTHS,
                registeredByMe
        );
    }
}
