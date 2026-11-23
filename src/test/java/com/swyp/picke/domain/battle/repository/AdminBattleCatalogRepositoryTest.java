package com.swyp.picke.domain.battle.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AdminBattleCatalogRepositoryTest {
    @Autowired private BattleRepository battleRepository;

    @Test
    void adminCatalogSummaryAndFilterQueriesReturnManageableContents() {
        String title = "관리 대상 콘텐츠 " + UUID.randomUUID();
        Battle battle = battleRepository.saveAndFlush(Battle.builder()
                .title(title)
                .summary("관리자 화면 검색 테스트")
                .description("콘텐츠 관리 목록")
                .status(BattleStatus.PUBLISHED)
                .creatorType(BattleCreatorType.ADMIN)
                .build());

        assertThat(battleRepository.countAdminCatalogContents()).isEqualTo(1);
        assertThat(battleRepository.countAdminCatalogContentsWithoutDifficulty()).isEqualTo(1);
        assertThat(battleRepository.countAdminCatalogContentsWithoutCategory()).isEqualTo(1);
        assertThat(battleRepository.countAdminCatalogContentsRecommendedForClass()).isZero();

        var results = battleRepository.searchAdminCatalogContents(title, null, null, false,
                PageRequest.of(0, 20));

        assertThat(results.getTotalElements()).isEqualTo(1);
        assertThat(results.getContent().getFirst().getId()).isEqualTo(battle.getId());
    }
}
