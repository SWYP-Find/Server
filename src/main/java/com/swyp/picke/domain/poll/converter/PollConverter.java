package com.swyp.picke.domain.poll.converter;

import com.swyp.picke.domain.admin.dto.poll.request.AdminPollCreateRequest;
import com.swyp.picke.domain.admin.dto.poll.response.AdminPollDetailResponse;
import com.swyp.picke.domain.poll.dto.response.PollDetailResponse;
import com.swyp.picke.domain.poll.dto.response.PollListResponse;
import com.swyp.picke.domain.poll.dto.response.PollOptionResponse;
import com.swyp.picke.domain.poll.dto.response.PollSimpleResponse;
import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.poll.entity.PollOption;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.util.ResourceUrlProvider;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PollConverter {

    private final ResourceUrlProvider urlProvider;

    private static final Comparator<PollOption> OPTION_SORTER =
            Comparator.comparing((PollOption option) -> option.getDisplayOrder() == null ? Integer.MAX_VALUE : option.getDisplayOrder())
                    .thenComparing(option -> option.getLabel() == null ? "" : option.getLabel().name())
                    .thenComparing(PollOption::getId);

    public Poll toEntity(AdminPollCreateRequest request) {
        return Poll.builder()
                .titlePrefix(request.titlePrefix())
                .titleSuffix(request.titleSuffix())
                .targetDate(request.targetDate())
                .publishAt(request.publishAt())
                .status(request.status())
                .build();
    }

    public PollListResponse toListResponse(Page<Poll> pollPage) {
        List<PollSimpleResponse> items = pollPage.getContent().stream()
                .map(this::toSimpleResponse)
                .toList();
        return new PollListResponse(items, pollPage.getNumber() + 1, pollPage.getTotalPages(), pollPage.getTotalElements());
    }

    public PollSimpleResponse toSimpleResponse(Poll poll) {
        return new PollSimpleResponse(
                poll.getId(),
                poll.getTitlePrefix(),
                poll.getTitleSuffix(),
                poll.getStatus(),
                poll.getPublishAt(),
                poll.getCreatedAt()
        );
    }

    public AdminPollDetailResponse toAdminDetailResponse(Poll poll, List<PollOption> options) {
        return new AdminPollDetailResponse(
                poll.getId(),
                poll.getTitlePrefix(),
                poll.getTitleSuffix(),
                poll.getTargetDate(),
                poll.getPublishAt(),
                poll.getStatus(),
                toOptionResponses(options)
        );
    }

    public PollDetailResponse toDetailResponse(Poll poll, List<PollOption> options) {
        return new PollDetailResponse(
                poll.getId(),
                poll.getTitlePrefix(),
                poll.getTitleSuffix(),
                poll.getTargetDate(),
                poll.getStatus(),
                toOptionResponses(options)
        );
    }

    private List<PollOptionResponse> toOptionResponses(List<PollOption> options) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .sorted(OPTION_SORTER)
                .map(option -> new PollOptionResponse(
                        option.getId(),
                        option.getLabel(),
                        option.getTitle(),
                        option.getDisplayOrder(),
                        option.getVoteCount(),
                        urlProvider.getImageUrl(FileCategory.PHILOSOPHER, option.getImageUrl())
                ))
                .toList();
    }
}
