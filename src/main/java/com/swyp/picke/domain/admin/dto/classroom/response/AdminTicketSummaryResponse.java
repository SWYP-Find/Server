package com.swyp.picke.domain.admin.dto.classroom.response;

public record AdminTicketSummaryResponse(
        long totalTickets,
        long inUse,
        long unregistered,
        long expiringWithin30Days
) {
}
