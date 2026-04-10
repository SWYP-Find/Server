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
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import java.util.List;

public interface BattleService {

    Battle findById(Long battleId);

    BattleOption findOptionById(Long optionId);

    BattleOption findOptionByBattleIdAndLabel(Long battleId, BattleOptionLabel label);

    List<TodayBattleResponse> getEditorPicks();

    List<TodayBattleResponse> getTrendingBattles();

    List<TodayBattleResponse> getBestBattles();

    List<TodayBattleResponse> getTodayPicks();

    List<TodayBattleResponse> getNewBattles(List<Long> excludeIds);

    BattleListResponse getBattles(int page, int size, String status);

    TodayBattleListResponse getTodayBattles();

    BattleUserDetailResponse getBattleDetail(Long battleId);

    BattleVoteResponse BattleVote(Long battleId, Long optionId);

    BattleScenarioResponse getBattleScenario(Long battleId);

    UserBattleStatusResponse getUserBattleStatus(Long battleId);

    AdminBattleDetailResponse createBattle(AdminBattleCreateRequest request, Long adminUserId);

    AdminBattleDetailResponse getAdminBattleDetail(Long battleId);

    AdminBattleDetailResponse updateBattle(Long battleId, AdminBattleUpdateRequest request);

    AdminBattleDeleteResponse deleteBattle(Long battleId);
}
