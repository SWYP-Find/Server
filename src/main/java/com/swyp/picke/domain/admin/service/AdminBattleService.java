package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleCreateRequest;
import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleUpdateRequest;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleDeleteResponse;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleDetailResponse;
import com.swyp.picke.domain.battle.service.BattleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminBattleService {

    private final BattleService battleService;

    public AdminBattleDetailResponse createBattle(AdminBattleCreateRequest request, Long adminUserId) {
        return battleService.createBattle(request, adminUserId);
    }

    public AdminBattleDetailResponse getBattleDetail(Long battleId) {
        return battleService.getAdminBattleDetail(battleId);
    }

    public AdminBattleDetailResponse updateBattle(Long battleId, AdminBattleUpdateRequest request) {
        return battleService.updateBattle(battleId, request);
    }

    public AdminBattleDeleteResponse deleteBattle(Long battleId) {
        return battleService.deleteBattle(battleId);
    }

    public Object getBattles(int page, int size, String status) {
        return battleService.getBattles(page, size, status);
    }
}