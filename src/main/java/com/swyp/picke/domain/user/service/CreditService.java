package com.swyp.picke.domain.user.service;

import com.swyp.picke.domain.user.entity.CreditHistory;
import com.swyp.picke.domain.user.enums.TierCode;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.repository.CreditHistoryRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditService {

    private final CreditHistoryRepository creditHistoryRepository;
    private final UserRepository userRepository;
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
     *
     * 적립이 성공하면 User.credit 캐시를 동기 증감하여 {@link #getTotalPoints}가 전체 히스토리를 재집계하지 않도록 한다.
     * (user, creditType, referenceId) 중복 시 조용히 무시(멱등).
     */
    @Transactional
    public void addCredit(Long userId, CreditType creditType, int amount, Long referenceId) {
        validateReferenceId(referenceId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        CreditHistory history = CreditHistory.builder()
                .user(user)
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
        if (userRepository.incrementCredit(userId, amount) == 0) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 유저의 현재 크레딧 잔액 조회.
     * 전체 CreditHistory 집계가 아닌 User.credit 캐시 필드를 읽는다.
     */
    public int getTotalPoints(Long userId) {
        Integer credit = userRepository.findCreditById(userId);
        if (credit == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        return credit;
    }

    public TierCode getTier(Long userId) {
        return TierCode.fromPoints(getTotalPoints(userId));
    }

    /**
     * 크레딧 적립/소비 내역 페이징 조회 (최신순).
     */
    public Page<CreditHistory> getHistory(Long userId, Pageable pageable) {
        return creditHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    private void validateReferenceId(Long referenceId) {
        if (referenceId == null) {
            throw new CustomException(ErrorCode.CREDIT_REFERENCE_REQUIRED);
        }
    }
}
