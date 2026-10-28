package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.classroom.dto.response.ClassInsightResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassMyReportResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassMyActivityResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassResultResponse;

/**
 * 클래스 결과·AI 리포트 조회 (PRD ⑤). 통계는 마감 전 실시간 / 마감 후 스냅샷, 리포트는 마감 후 1회 생성된 것을 읽는다.
 * 읽기는 운영자·ACTIVE 멤버, 이용권 종료 + 30일까지.
 */
public interface ClassResultService {

    ClassResultResponse getResult(Long userId, Long classId);

    ClassMyReportResponse getMyReport(Long userId, Long classId);

    ClassMyActivityResponse getMyActivity(Long userId, Long classId, String cursor, Integer size);

    ClassInsightResponse getInsight(Long userId, Long classId);

    ClassInsightResponse retryInsight(Long userId, Long classId);
}
