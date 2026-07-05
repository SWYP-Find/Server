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

    private static final int PUBLISH_BATCH_SIZE = 100;

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
        Page<Poll> pollPage = pollRepository.findByStatusOrderByCreatedAtDesc(
                PollStatus.PUBLISHED,
                PageRequest.of(pageNumber, size)
        );
        return pollConverter.toListResponse(pollPage);
    }

    @Override
    public PollListResponse getPolls(int page, int size, String status) {
        int pageNumber = Math.max(0, page - 1);
        PageRequest pageRequest = PageRequest.of(pageNumber, size);
        PollStatus pollStatus = parsePollStatus(status);

        Page<Poll> pollPage = pollStatus == null
                ? pollRepository.findAllByOrderByCreatedAtDesc(pageRequest)
                : pollRepository.findByStatusOrderByCreatedAtDesc(pollStatus, pageRequest);

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
        if (poll.getStatus() != PollStatus.PUBLISHED) {
            throw new CustomException(ErrorCode.BATTLE_NOT_FOUND);
        }
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
                        .imageUrl(optionRequest.imageUrl())
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
        poll.updatePublishAt(request.publishAt());

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
                            .imageUrl(optionRequest.imageUrl())
                            .build();
                    option = pollOptionRepository.save(option);
                } else {
                    option.update(optionRequest.title(), displayOrder, optionRequest.imageUrl());
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

    @Override
    @Transactional
    public int openReadyPolls(LocalDateTime now) {
        int openedCount = 0;
        PageRequest pageRequest = PageRequest.of(0, PUBLISH_BATCH_SIZE);

        while (true) {
            Page<Poll> readyPage = pollRepository.findByStatusAndPublishAtLessThanEqual(
                    PollStatus.PENDING,
                    now,
                    pageRequest
            );
            if (readyPage.isEmpty()) {
                break;
            }

            readyPage.getContent().forEach(Poll::publish);
            openedCount += readyPage.getNumberOfElements();
            pollRepository.flush();
        }

        return openedCount;
    }

    private int resolveDisplayOrder(Integer requestedOrder, int fallbackOrder) {
        if (requestedOrder == null || requestedOrder < 1) {
            return fallbackOrder;
        }
        return requestedOrder;
    }

    private PollStatus parsePollStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }

        try {
            return PollStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
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

