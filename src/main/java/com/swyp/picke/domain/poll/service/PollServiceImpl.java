package com.swyp.picke.domain.poll.service;

import com.swyp.picke.domain.poll.converter.PollConverter;
import com.swyp.picke.domain.admin.dto.poll.request.AdminPollCreateRequest;
import com.swyp.picke.domain.admin.dto.poll.request.AdminPollOptionRequest;
import com.swyp.picke.domain.admin.dto.poll.request.AdminPollUpdateRequest;
import com.swyp.picke.domain.admin.dto.poll.response.AdminPollDeleteResponse;
import com.swyp.picke.domain.admin.dto.poll.response.AdminPollDetailResponse;
import com.swyp.picke.domain.poll.dto.response.PollDetailResponse;
import com.swyp.picke.domain.poll.dto.response.PollListResponse;
import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.poll.entity.PollOption;
import com.swyp.picke.domain.poll.enums.PollOptionLabel;
import com.swyp.picke.domain.poll.enums.PollStatus;
import com.swyp.picke.domain.poll.repository.PollOptionRepository;
import com.swyp.picke.domain.poll.repository.PollRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PollServiceImpl implements PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollConverter pollConverter;

    @Override
    public Poll findById(Long pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));
    }

    @Override
    public PollListResponse getPolls(int page, int size) {
        int pageNumber = Math.max(0, page - 1);
        Page<Poll> pollPage = pollRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(pageNumber, size));
        return pollConverter.toListResponse(pollPage);
    }

    @Override
    @Transactional
    public List<Poll> getTodayPicks(int limit) {
        int safeLimit = Math.max(1, limit);
        LocalDate today = LocalDate.now();

        ensureTodayPicks(today, safeLimit);
        return pollRepository.findTodayPicks(PollStatus.PUBLISHED, today, PageRequest.of(0, safeLimit));
    }

    @Override
    public List<PollOption> getOptions(Poll poll) {
        return pollOptionRepository.findByPollOrderByDisplayOrderAscLabelAscIdAsc(poll);
    }

    @Override
    public long countVotes(Poll poll) {
        return poll.getTotalParticipantsCount() == null ? 0L : poll.getTotalParticipantsCount();
    }

    @Override
    public PollDetailResponse getPollDetail(Long pollId) {
        Poll poll = findById(pollId);
        List<PollOption> options = pollOptionRepository.findByPollOrderByDisplayOrderAscLabelAscIdAsc(poll);
        return pollConverter.toDetailResponse(poll, options);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public AdminPollDetailResponse getAdminPollDetail(Long pollId) {
        Poll poll = findById(pollId);
        List<PollOption> options = pollOptionRepository.findByPollOrderByDisplayOrderAscLabelAscIdAsc(poll);
        return pollConverter.toAdminDetailResponse(poll, options);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminPollDetailResponse createPoll(AdminPollCreateRequest request) {
        Poll poll = pollConverter.toEntity(request);
        poll = pollRepository.save(poll);

        List<PollOption> savedOptions = new ArrayList<>();
        if (request.options() != null) {
            for (int i = 0; i < request.options().size(); i++) {
                AdminPollOptionRequest optionRequest = request.options().get(i);
                int displayOrder = resolveDisplayOrder(optionRequest.displayOrder(), i + 1);
                PollOption option = PollOption.builder()
                        .poll(poll)
                        .label(optionRequest.label())
                        .title(optionRequest.title())
                        .displayOrder(displayOrder)
                        .build();
                option = pollOptionRepository.save(option);
                savedOptions.add(option);
            }
        }

        return pollConverter.toAdminDetailResponse(poll, savedOptions);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminPollDetailResponse updatePoll(Long pollId, AdminPollUpdateRequest request) {
        Poll poll = findById(pollId);
        poll.update(
                request.titlePrefix(),
                request.titleSuffix(),
                request.targetDate(),
                request.status()
        );

        if (request.options() != null) {
            List<PollOption> existingOptions = pollOptionRepository.findByPollOrderByDisplayOrderAscLabelAscIdAsc(poll);
            Map<PollOptionLabel, PollOption> existingOptionMap = new HashMap<>();
            for (PollOption option : existingOptions) {
                existingOptionMap.put(option.getLabel(), option);
            }

            Set<PollOptionLabel> requestedLabels = new HashSet<>();
            for (int i = 0; i < request.options().size(); i++) {
                AdminPollOptionRequest optionRequest = request.options().get(i);
                int displayOrder = resolveDisplayOrder(optionRequest.displayOrder(), i + 1);
                requestedLabels.add(optionRequest.label());
                PollOption option = existingOptionMap.get(optionRequest.label());

                if (option == null) {
                    option = PollOption.builder()
                            .poll(poll)
                            .label(optionRequest.label())
                            .title(optionRequest.title())
                            .displayOrder(displayOrder)
                            .build();
                    option = pollOptionRepository.save(option);
                } else {
                    option.update(optionRequest.title(), displayOrder);
                }
            }

            for (PollOption existingOption : existingOptions) {
                if (requestedLabels.contains(existingOption.getLabel())) continue;
                pollOptionRepository.delete(existingOption);
            }
        }

        return getAdminPollDetail(pollId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminPollDeleteResponse deletePoll(Long pollId) {
        Poll poll = findById(pollId);
        List<PollOption> options = pollOptionRepository.findByPollOrderByDisplayOrderAscLabelAscIdAsc(poll);
        pollOptionRepository.deleteAll(options);
        pollRepository.delete(poll);
        return new AdminPollDeleteResponse(true, LocalDateTime.now());
    }

    private int resolveDisplayOrder(Integer requestedOrder, int fallbackOrder) {
        if (requestedOrder == null || requestedOrder < 1) {
            return fallbackOrder;
        }
        return requestedOrder;
    }

    private void ensureTodayPicks(LocalDate today, int requiredCount) {
        List<Poll> todays = pollRepository.findTodayPicks(PollStatus.PUBLISHED, today, PageRequest.of(0, requiredCount));
        int missingCount = requiredCount - todays.size();
        if (missingCount <= 0) return;

        List<Poll> candidates = pollRepository.findAutoAssignableTodayPicks(
                PollStatus.PUBLISHED,
                today,
                PageRequest.of(0, missingCount)
        );
        for (Poll candidate : candidates) {
            candidate.update(null, null, today, null);
        }
    }
}

