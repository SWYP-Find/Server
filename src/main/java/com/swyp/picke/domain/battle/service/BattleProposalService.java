package com.swyp.picke.domain.battle.service;

import com.swyp.picke.domain.battle.dto.request.BattleProposalRequest;
import com.swyp.picke.domain.battle.dto.request.BattleProposalReviewRequest;
import com.swyp.picke.domain.battle.dto.response.BattleProposalResponse;
import com.swyp.picke.domain.battle.entity.BattleProposal;
import com.swyp.picke.domain.battle.enums.BattleProposalStatus;
import com.swyp.picke.domain.battle.repository.BattleProposalRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleProposalService {

    private final BattleProposalRepository battleProposalRepository;
    private final CreditService creditService;
    private final UserService userService;

    @Transactional
    public BattleProposalResponse propose(BattleProposalRequest request) {
        User user = userService.findCurrentUser();

        int cost = CreditType.TOPIC_SUGGEST.getDefaultAmount();

        int totalCredits = creditService.getTotalPoints(user.getId());
        if (totalCredits < cost) {
            throw new CustomException(ErrorCode.CREDIT_NOT_ENOUGH);
        }

        BattleProposal proposal = BattleProposal.builder()
                .user(user)
                .category(request.getCategory())
                .topic(request.getTopic())
                .positionA(request.getPositionA())
                .positionB(request.getPositionB())
                .description(request.getDescription())
                .build();

        battleProposalRepository.save(proposal);

        creditService.addCredit(user.getId(), CreditType.TOPIC_SUGGEST, -cost, proposal.getId());

        return new BattleProposalResponse(proposal);
    }

    public PageResponse<BattleProposalResponse> getProposals(int page, int size, String status) {
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        BattleProposalStatus proposalStatus = (status != null && !status.isEmpty())
                ? BattleProposalStatus.valueOf(status.toUpperCase())
                : null;

        Page<BattleProposal> proposals = (proposalStatus != null)
                ? battleProposalRepository.findAllByStatus(proposalStatus, pageable)
                : battleProposalRepository.findAll(pageable);

        return PageResponse.of(proposals.map(BattleProposalResponse::new));
    }

    @Transactional
    public BattleProposalResponse review(Long proposalId, BattleProposalReviewRequest request) {
        BattleProposal proposal = battleProposalRepository.findById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        if (proposal.getStatus() != BattleProposalStatus.PENDING) {
            throw new CustomException(ErrorCode.BATTLE_ALREADY_PUBLISHED);
        }

        if (request.getAction() == BattleProposalReviewRequest.Action.ACCEPT) {
            proposal.accept();
            int reward = CreditType.TOPIC_ADOPTED.getDefaultAmount();
            creditService.addCredit(proposal.getUser().getId(), CreditType.TOPIC_ADOPTED, reward, proposalId);
        } else {
            proposal.reject();
        }

        return new BattleProposalResponse(proposal);
    }
}
