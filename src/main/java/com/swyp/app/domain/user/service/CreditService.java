package com.swyp.app.domain.user.service;

import com.swyp.app.domain.user.entity.CreditHistory;
import com.swyp.app.domain.user.entity.TierCode;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.enums.CreditType;
import com.swyp.app.domain.user.repository.CreditHistoryRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditService {

    private final CreditHistoryRepository creditHistoryRepository;
    private final UserService userService;

    /**
     * 현재 로그인한 유저에게 크레딧 적립 (기본 포인트).
     * 일반적인 유저 액션(투표, 관점 작성, 좋아요 등)에서 사용.
     * 예: creditService.addCredit(CreditType.BATTLE_VOTE, voteId);
     */
    @Transactional
    public void addCredit(CreditType creditType, Long referenceId) {
        User user = userService.findCurrentUser();
        addCredit(user.getId(), creditType, creditType.getDefaultAmount(), referenceId);
    }

    /**
     * 특정 유저에게 크레딧 적립 (기본 포인트).
     * SecurityContext 없이 호출하는 경우(배치, 스케줄러, 관리자 지급 등)에서 사용.
     * 예: creditService.addCredit(authorId, CreditType.BEST_COMMENT, perspectiveId);
     */
    @Transactional
    public void addCredit(Long userId, CreditType creditType, Long referenceId) {
        addCredit(userId, creditType, creditType.getDefaultAmount(), referenceId);
    }

    /**
     * 특정 유저에게 커스텀 포인트로 크레딧 적립.
     * CreditType의 기본 포인트가 아닌 가변 포인트가 필요한 경우(FREE_CHARGE 랜덤 박스 등)에서 사용.
     * 예: creditService.addCredit(userId, CreditType.FREE_CHARGE, 15, boxId);
     */
    @Transactional
    public void addCredit(Long userId, CreditType creditType, int amount, Long referenceId) {
        validateReferenceId(referenceId);

        CreditHistory history = CreditHistory.builder()
                .userId(userId)
                .creditType(creditType)
                .amount(amount)
                .referenceId(referenceId)
                .build();

        try {
            creditHistoryRepository.saveAndFlush(history);
        } catch (DataIntegrityViolationException e) {
            if (creditHistoryRepository.existsByUserIdAndCreditTypeAndReferenceId(userId, creditType, referenceId)) {
                return;
            }
            throw new CustomException(ErrorCode.CREDIT_SAVE_FAILED);
        }
    }

    public int getTotalPoints(Long userId) {
        return creditHistoryRepository.sumAmountByUserId(userId);
    }

    public TierCode getTier(Long userId) {
        return TierCode.fromPoints(getTotalPoints(userId));
    }

    private void validateReferenceId(Long referenceId) {
        if (referenceId == null) {
            throw new CustomException(ErrorCode.CREDIT_REFERENCE_REQUIRED);
        }
    }
}
