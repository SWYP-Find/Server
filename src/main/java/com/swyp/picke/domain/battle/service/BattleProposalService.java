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

    private static final int PROPOSAL_COST = 30;
    private static final int PROPOSAL_REWARD = 100;

    @Transactional
    public BattleProposalResponse propose(BattleProposalRequest request) {
        User user = userService.findCurrentUser();

        // 크레딧 잔액 확인
        int totalCredits = creditService.getTotalPoints(user.getId());
        if (totalCredits < PROPOSAL_COST) {
            throw new CustomException(ErrorCode.CREDIT_NOT_ENOUGH);
        }

        // 제안 저장
        BattleProposal proposal = BattleProposal.builder()
                .user(user)
                .category(request.getCategory())
                .topic(request.getTopic())
                .positionA(request.getPositionA())
                .positionB(request.getPositionB())
                .description(request.getDescription())
                .build();

        battleProposalRepository.save(proposal);

        // 30크레딧 차감 (음수로 저장)
        creditService.addCredit(user.getId(), CreditType.TOPIC_SUGGEST, -PROPOSAL_COST, proposal.getId());

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
            // 100크레딧 지급
            creditService.addCredit(proposal.getUser().getId(), CreditType.TOPIC_ADOPTED, PROPOSAL_REWARD, proposalId);
        } else {
            proposal.reject();
        }

        return new BattleProposalResponse(proposal);
    }
}
