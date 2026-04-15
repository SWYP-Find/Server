package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.poll.entity.Poll;
import com.swyp.picke.domain.poll.entity.PollOption;
import com.swyp.picke.domain.poll.repository.PollOptionRepository;
import com.swyp.picke.domain.poll.service.PollService;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.vote.dto.request.PollVoteRequest;
import com.swyp.picke.domain.vote.dto.response.PollVoteResponse;
import com.swyp.picke.domain.vote.entity.PollVote;
import com.swyp.picke.domain.vote.repository.PollVoteRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PollVoteServiceImpl implements PollVoteService {

    private final PollService pollService;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PollVoteResponse submitPoll(Long battleId, Long userId, PollVoteRequest request) {
        Long pollId = battleId;
        Poll poll = pollService.findById(pollId);

        PollOption selectedOption = pollOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        if (!selectedOption.getPoll().getId().equals(poll.getId())) {
            throw new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND);
        }

        PollVote pollVote = saveOrUpdate(poll, userId, selectedOption);
        long totalCount = poll.getTotalParticipantsCount() == null ? 0L : poll.getTotalParticipantsCount();

        return new PollVoteResponse(
                pollId,
                pollVote.getSelectedOption().getId(),
                totalCount,
                buildStats(poll, totalCount, true)
        );
    }

    @Override
    public PollVoteResponse getMyPollVote(Long battleId, Long userId) {
        Long pollId = battleId;
        Poll poll = pollService.findById(pollId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        long totalCount = poll.getTotalParticipantsCount() == null ? 0L : poll.getTotalParticipantsCount();

        return pollVoteRepository.findByPollAndUser(poll, user)
                .map(pollVote -> new PollVoteResponse(
                        pollId,
                        pollVote.getSelectedOption().getId(),
                        totalCount,
                        buildStats(poll, totalCount, true)
                ))
                .orElseGet(() -> new PollVoteResponse(
                        pollId,
                        null,
                        totalCount,
                        buildStats(poll, totalCount, false)
                ));
    }

    @Override
    @Transactional
    public void deletePollVoteByBattleId(Long battleId) {
        Long pollId = battleId;
        Poll poll = pollService.findById(pollId);

        List<PollVote> votes = pollVoteRepository.findAllByPoll(poll);
        for (PollVote pollVote : votes) {
            poll.decreaseTotalParticipantsCount();
            if (pollVote.getSelectedOption() != null) {
                pollVote.getSelectedOption().decreaseVoteCount();
            }
        }
        pollVoteRepository.deleteAllInBatch(votes);
    }

    private PollVote saveOrUpdate(Poll poll, Long userId, PollOption selectedOption) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return pollVoteRepository.findByPollAndUser(poll, user)
                .map(pollVote -> {
                    if (!pollVote.getSelectedOption().equals(selectedOption)) {
                        pollVote.getSelectedOption().decreaseVoteCount();
                        selectedOption.increaseVoteCount();
                        pollVote.updateOption(selectedOption);
                    }
                    return pollVote;
                })
                .orElseGet(() -> {
                    selectedOption.increaseVoteCount();
                    poll.increaseTotalParticipantsCount();
                    return pollVoteRepository.save(
                            PollVote.builder()
                                    .user(user)
                                    .poll(poll)
                                    .selectedOption(selectedOption)
                                    .build()
                    );
                });
    }

    private List<PollVoteResponse.OptionStat> buildStats(Poll poll, long totalCount, boolean revealCounts) {
        return pollOptionRepository.findByPollOrderByDisplayOrderAscLabelAscIdAsc(poll).stream()
                .map(option -> {
                    long count = revealCounts ? (option.getVoteCount() == null ? 0L : option.getVoteCount()) : 0L;
                    double ratio = (!revealCounts || totalCount == 0)
                            ? 0.0
                            : Math.round((double) count / totalCount * 1000) / 10.0;

                    return new PollVoteResponse.OptionStat(
                            option.getId(),
                            option.getLabel().name(),
                            option.getTitle(),
                            count,
                            ratio
                    );
                })
                .toList();
    }
}

