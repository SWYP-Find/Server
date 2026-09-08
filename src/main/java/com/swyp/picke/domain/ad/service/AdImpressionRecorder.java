package com.swyp.picke.domain.ad.service;

import com.swyp.picke.domain.ad.entity.AdImpressionDaily;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.repository.AdImpressionDailyRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일별 노출 집계 행을 쓰는 지점.
 * 갱신과 삽입을 각각 독립 트랜잭션으로 나눠 둔다.
 *
 * <p>PostgreSQL은 한 문장이 제약을 위반하면 그 트랜잭션 전체가 abort 상태로 들어가,
 * 뒤따르는 쿼리가 모두 {@code current transaction is aborted}로 실패한다.
 * 삽입 실패를 같은 트랜잭션 안에서 갱신으로 되돌리려 하면 운영에서 그 복구가 동작하지 않고,
 * 예외가 위로 새면 호출부가 {@code UnexpectedRollbackException}을 맞아 배치 전체가 날아간다.
 * 그래서 삽입만 별도 트랜잭션에 가둬 실패를 격리한다.
 */
@Component
@RequiredArgsConstructor
public class AdImpressionRecorder {

    private final AdImpressionDailyRepository adImpressionDailyRepository;

    /** 이미 있는 행을 올린다. 대상 행이 없으면 false. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean increment(Long creativeId, AdSlotCode slot, LocalDate statDate) {
        return adImpressionDailyRepository.increment(creativeId, slot, statDate, 1L) > 0;
    }

    /**
     * 오늘 첫 노출이라 행을 만든다.
     * 같은 (소재, 지면, 날짜)를 다른 요청이 먼저 만들었으면 제약 위반으로 실패하고,
     * 실패는 이 트랜잭션 안에서 끝난다. 되돌리기는 호출부가 갱신으로 처리한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(Long creativeId, AdSlotCode slot, LocalDate statDate) {
        adImpressionDailyRepository.saveAndFlush(AdImpressionDaily.builder()
                .creativeId(creativeId)
                .slot(slot)
                .statDate(statDate)
                .impressions(1L)
                .build());
    }
}
