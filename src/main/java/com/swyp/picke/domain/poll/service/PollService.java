package com.swyp.picke.domain.poll.service;

import com.swyp.picke.domain.admin.dto.poll.request.AdminPollCreateRequest;
import com.swyp.picke.domain.admin.dto.poll.request.AdminPollUpdateRequest;
import com.swyp.picke.domain.admin.dto.poll.response.AdminPollDeleteResponse;
import com.swyp.picke.domain.admin.dto.poll.response.AdminPollDetailResponse;
import com.swyp.picke.domain.poll.dto.response.PollDetailResponse;
import com.swyp.picke.domain.poll.dto.response.PollListResponse;
import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.poll.entity.PollOption;
import java.util.List;

public interface PollService {
    Poll findById(Long pollId);

    PollListResponse getPolls(int page, int size);

    List<Poll> getTodayPicks(int limit);

    List<PollOption> getOptions(Poll poll);

    long countVotes(Poll poll);

    PollDetailResponse getPollDetail(Long pollId);

    AdminPollDetailResponse getAdminPollDetail(Long pollId);

    AdminPollDetailResponse createPoll(AdminPollCreateRequest request);

    AdminPollDetailResponse updatePoll(Long pollId, AdminPollUpdateRequest request);

    AdminPollDeleteResponse deletePoll(Long pollId);
}


