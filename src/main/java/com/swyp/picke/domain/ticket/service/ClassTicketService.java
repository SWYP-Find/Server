package com.swyp.picke.domain.ticket.service;

import com.swyp.picke.domain.ticket.dto.request.ClassTicketRegisterRequest;
import com.swyp.picke.domain.ticket.dto.response.ClassTicketResponse;

public interface ClassTicketService {

    ClassTicketResponse register(Long userId, ClassTicketRegisterRequest request);

    ClassTicketResponse getMyTicket(Long userId);
}
