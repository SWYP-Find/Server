package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.classroom.dto.response.ClassBattleDetailResponse;
import com.swyp.picke.domain.perspective.dto.request.CreatePerspectiveRequest;
import com.swyp.picke.domain.perspective.dto.response.CreatePerspectiveResponse;
import com.swyp.picke.domain.perspective.dto.response.MyPerspectiveResponse;
import com.swyp.picke.domain.perspective.dto.response.PerspectiveListResponse;
import com.swyp.picke.domain.vote.dto.request.VoteRequest;
import com.swyp.picke.domain.vote.dto.response.MyVoteResponse;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.dto.response.VoteStatsResponse;
import java.util.List;

/**
 * 클래스 안에서의 배틀 진행(PRD ④). 배틀 ID 는 클래스에 붙은 것을 쓰고, 모든 투표·의견은 class_id 로 스코핑된다.
 * 읽기는 운영자/ACTIVE 멤버, 쓰기는 ACTIVE 멤버가 마감 전에만.
 */
public interface ClassBattleService {

    ClassBattleDetailResponse getDetail(Long userId, Long classId);

    VoteResultResponse preVote(Long userId, Long classId, VoteRequest request);

    void completeTts(Long userId, Long classId);

    VoteResultResponse midVote(Long userId, Long classId, VoteRequest request);

    VoteResultResponse postVote(Long userId, Long classId, VoteRequest request);

    MyVoteResponse getMyVote(Long userId, Long classId);

    VoteStatsResponse getVoteStats(Long userId, Long classId);

    CreatePerspectiveResponse createPerspective(Long userId, Long classId, CreatePerspectiveRequest request);

    PerspectiveListResponse getPerspectives(Long userId, Long classId, String cursor, Integer size, Long optionId, String sort);

    List<MyPerspectiveResponse> getMyPerspectives(Long userId, Long classId);
}
