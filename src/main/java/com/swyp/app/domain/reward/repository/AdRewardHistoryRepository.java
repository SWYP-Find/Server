package com.swyp.app.domain.reward.repository;

import com.swyp.app.domain.reward.entity.AdRewardHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdRewardHistoryRepository extends JpaRepository<AdRewardHistory, Long> {

    /**
     * 1. 중복 보상 지급 방지를 위한 검증 메서드
     * @param transactionId 구글에서 보낸 고유 트랜잭션 ID
     * @return 존재하면 true, 없으면 false
     */

    // transactionId는 한 광고의 시청 영수증 번호라고 생각해주세요!
    boolean existsByTransactionId(String transactionId);

}
