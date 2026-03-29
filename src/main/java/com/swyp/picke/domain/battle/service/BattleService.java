package com.swyp.picke.domain.battle.service;

import com.swyp.picke.domain.battle.dto.request.AdminBattleCreateRequest;
import com.swyp.picke.domain.battle.dto.request.AdminBattleUpdateRequest;
import com.swyp.picke.domain.battle.dto.response.*;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.enums.BattleType;

import java.util.List;

public interface BattleService {

    // === [내부 공통/조회 메서드] ===
    Battle findById(Long battleId);
    BattleOption findOptionById(Long optionId);
    BattleOption findOptionByBattleIdAndLabel(Long battleId, BattleOptionLabel label);

    // === [사용자용 - 홈 화면 5단 로직 지원 API] ===

    // 1. 에디터 픽 조회 (isEditorPick = true)
    List<TodayBattleResponse> getEditorPicks(int limit);

    // 2. 지금 뜨는 배틀 조회 (최근 24시간 투표 급증순)
    List<TodayBattleResponse> getTrendingBattles(int limit);

    // 3. Best 배틀 조회 (누적 지표 랭킹)
    List<TodayBattleResponse> getBestBattles(int limit);

    // 4. 오늘의 Pické 조회 (단일 타입 매칭)
    List<TodayBattleResponse> getTodayPicks(BattleType type, int limit);

    // 5. 새로운 배틀 조회 (중복 제외 리스트)
    List<TodayBattleResponse> getNewBattles(List<Long> excludeIds, int limit);


    // === [사용자용 - 기본 API] ===

    // 전체 배틀 목록 페이징 조회
    BattleListResponse getBattles(int page, int size, String type);

    // 오늘의 배틀 (기존 로직 유지용)
    TodayBattleListResponse getTodayBattles();

    // 배틀 상세 정보
    BattleUserDetailResponse getBattleDetail(Long battleId);

    // 투표 실행 및 실시간 통계 결과 반환
    BattleVoteResponse vote(Long battleId, Long optionId);


    // === [관리자용 API] ===

    // 배틀 생성
    AdminBattleDetailResponse createBattle(AdminBattleCreateRequest request, Long adminUserId);

    // 배틀 수정
    AdminBattleDetailResponse updateBattle(Long battleId, AdminBattleUpdateRequest request);

    // 배틀 삭제 (DB에서 지우지 않고 소프트 딜리트/상태변경을 수행합니다)
    AdminBattleDeleteResponse deleteBattle(Long battleId);
}
