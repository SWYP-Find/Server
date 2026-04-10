package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.poll.request.AdminPollCreateRequest;
import com.swyp.picke.domain.admin.dto.poll.request.AdminPollUpdateRequest;
import com.swyp.picke.domain.admin.dto.poll.response.AdminPollDeleteResponse;
import com.swyp.picke.domain.admin.dto.poll.response.AdminPollDetailResponse;
import com.swyp.picke.domain.poll.service.PollService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminPollService {

    private final PollService pollService;

    public AdminPollDetailResponse createPoll(AdminPollCreateRequest request) {
        return pollService.createPoll(request);
    }

    public AdminPollDetailResponse getPollDetail(Long pollId) {
        return pollService.getAdminPollDetail(pollId);
    }

    public AdminPollDetailResponse updatePoll(Long pollId, AdminPollUpdateRequest request) {
        return pollService.updatePoll(pollId, request);
    }

    public AdminPollDeleteResponse deletePoll(Long pollId) {
        return pollService.deletePoll(pollId);
    }

    public Object getPolls(int page, int size, String status) {
        return pollService.getPolls(page, size);
    }
}