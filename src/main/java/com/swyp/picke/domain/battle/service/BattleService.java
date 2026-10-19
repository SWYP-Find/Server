package com.swyp.picke.domain.battle.service;

import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleCreateRequest;
import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleUpdateRequest;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleDeleteResponse;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleDetailResponse;
import com.swyp.picke.domain.battle.dto.response.BattleListResponse;
import com.swyp.picke.domain.battle.dto.response.BattleScenarioResponse;
import com.swyp.picke.domain.battle.dto.response.BattleUserDetailResponse;
import com.swyp.picke.domain.battle.dto.response.BattleVoteResponse;
import com.swyp.picke.domain.battle.dto.response.TodayBattleListResponse;
import com.swyp.picke.domain.battle.dto.response.TodayBattleResponse;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.enums.VoteSide;
import java.time.LocalDateTime;
import java.util.List;

public interface BattleService {

    Battle findById(Long battleId);

    BattleOption findOptionById(Long optionId);

    List<TodayBattleResponse> getEditorPicks();

    List<TodayBattleResponse> getTrendingBattles();

    List<TodayBattleResponse> getBestBattles();

    List<TodayBattleResponse> getTodayPicks();

    List<TodayBattleResponse> getNewBattles(List<Long> excludeIds);

    BattleListResponse getPublishedBattles(int page, int size);

    BattleListResponse getBattles(int page, int size, String status);

    TodayBattleListResponse getTodayBattles();

    BattleUserDetailResponse getBattleDetail(Long battleId);

    /** 조회수·현재 로그인 사용자와 무관하게, 주어진 투표 상태·단계·참여자 수로 배틀 상세를 조립한다 (클래스 배틀 상세에서 재사용) */
    BattleUserDetailResponse buildUserDetail(Battle battle, Long participantsCount, VoteSide voteSide, UserBattleStep step);

    BattleVoteResponse BattleVote(Long battleId, Long optionId);

    BattleScenarioResponse getBattleScenario(Long battleId);

    UserBattleStatusResponse getUserBattleStatus(Long battleId);

    AdminBattleDetailResponse createBattle(AdminBattleCreateRequest request, Long adminUserId);

    AdminBattleDetailResponse getAdminBattleDetail(Long battleId);

    AdminBattleDetailResponse updateBattle(Long battleId, AdminBattleUpdateRequest request);

    AdminBattleDeleteResponse deleteBattle(Long battleId);

    void seedBattle(Long battleId, int botCount);

    int openReadyBattles(LocalDateTime now);
}
