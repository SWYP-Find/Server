package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleCatalogItemResponse;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleCatalogSummaryResponse;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 콘텐츠 관리 화면에 필요한 관리자 배틀 목록과 카드 통계. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBattleCatalogService {
    private static final int MAX_PAGE_SIZE = 100;

    private final BattleRepository battleRepository;

    public AdminBattleCatalogSummaryResponse getSummary() {
        return new AdminBattleCatalogSummaryResponse(
                battleRepository.countAdminCatalogContents(),
                battleRepository.countAdminCatalogContentsWithoutDifficulty(),
                battleRepository.countAdminCatalogContentsWithoutCategory(),
                battleRepository.countAdminCatalogContentsRecommendedForClass());
    }

    public PageResponse<AdminBattleCatalogItemResponse> getContents(String keyword,
                                                                      BattleDifficulty difficulty,
                                                                      BattleCategory category,
                                                                      Boolean classRecommendable,
                                                                      int page,
                                                                      int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
        var result = battleRepository.searchAdminCatalogContents(normalizedKeyword, difficulty, category,
                classRecommendable, PageRequest.of(page, size));
        return PageResponse.of(result.map(this::toItem));
    }

    private AdminBattleCatalogItemResponse toItem(Battle battle) {
        return new AdminBattleCatalogItemResponse(battle.getId(), battle.getTitle(), battle.getDifficulty(),
                battle.getCategory(), battle.isClassRecommendable(), battle.getStatus(), battle.getCreatedAt());
    }
}
