package com.swyp.picke.domain.ticket.service;

import com.swyp.picke.domain.ticket.dto.request.ClassTicketRegisterRequest;
import com.swyp.picke.domain.ticket.dto.response.ClassTicketPreviewResponse;
import com.swyp.picke.domain.ticket.dto.response.ClassTicketResponse;

public interface ClassTicketService {

    /** 코드 확인: 소비 없이 정원·등록 기한을 돌려준다. 등록 불가 사유는 register 와 같은 에러 코드 */
    ClassTicketPreviewResponse preview(Long userId, String code);

    ClassTicketResponse register(Long userId, ClassTicketRegisterRequest request);

    ClassTicketResponse getMyTicket(Long userId);
}
