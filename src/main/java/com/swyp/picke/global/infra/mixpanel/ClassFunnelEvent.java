package com.swyp.picke.global.infra.mixpanel;

import java.time.Instant;
import java.util.Map;

/**
 * 클래스 퍼널 이벤트 1건 (#689). 트랜잭션이 커밋된 뒤 {@link ClassFunnelEventListener} 가 Mixpanel 로 보낸다.
 *
 * @param event      Mixpanel 이벤트 이름 (예: class_join_complete)
 * @param userId     행동한 사용자. distinct_id 는 이 사용자의 userTag
 * @param classId    대상 클래스. 개설 전 단계(class_create_start)는 null
 * @param properties 이벤트별 추가 속성 (round, report_type 등)
 * @param occurredAt 실제 발생 시각
 */
public record ClassFunnelEvent(String event, Long userId, Long classId, Map<String, Object> properties, Instant occurredAt) {
}
