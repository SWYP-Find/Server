package com.swyp.picke.domain.classroom.service;

/**
 * 클래스 인원 수 두 가지 (PRD §15 "참여 4명 / 사용 좌석 5석").
 *
 * @param seats         좌석을 차지한 인원. 활동 후 나가거나 내보내진 멤버도 포함하며 정원 판정에 쓴다
 * @param activeMembers 현재 명단 인원(ACTIVE 멤버). 참여하는 운영자를 포함한다
 */
public record ClassMemberCounts(long seats, long activeMembers) {

    public static final ClassMemberCounts EMPTY = new ClassMemberCounts(0, 0);
}
