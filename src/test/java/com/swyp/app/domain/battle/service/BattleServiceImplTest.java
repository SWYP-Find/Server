package com.swyp.app.domain.battle.service;

import com.swyp.app.domain.battle.converter.BattleConverter;
import com.swyp.app.domain.battle.dto.response.BattleSummaryResponse;
import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.entity.BattleTag;
import com.swyp.app.domain.battle.enums.BattleStatus;
import com.swyp.app.domain.battle.enums.BattleType;
import com.swyp.app.domain.battle.repository.BattleOptionRepository;
import com.swyp.app.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.app.domain.battle.repository.BattleRepository;
import com.swyp.app.domain.battle.repository.BattleTagRepository;
import com.swyp.app.domain.tag.entity.Tag;
import com.swyp.app.domain.tag.repository.TagRepository;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.domain.vote.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleServiceImplTest {

    @Mock
    private BattleRepository battleRepository;
    @Mock
    private BattleOptionRepository battleOptionRepository;
    @Mock
    private BattleTagRepository battleTagRepository;
    @Mock
    private BattleOptionTagRepository battleOptionTagRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private BattleConverter battleConverter;

    @InjectMocks
    private BattleServiceImpl battleService;

    private Battle battle;

    @BeforeEach
    void setUp() {
        battle = Battle.builder()
                .title("battle")
                .type(BattleType.BATTLE)
                .targetDate(LocalDate.now())
                .status(BattleStatus.PUBLISHED)
                .build();
    }

    @Test
    void getHomeTrendingBattles_요약응답으로_변환한다() {
        BattleSummaryResponse summary = new BattleSummaryResponse(
                UUID.randomUUID(),
                "trending",
                "summary",
                "thumbnail",
                BattleType.BATTLE,
                12,
                34L,
                56,
                List.of(),
                List.of()
        );

        when(battleRepository.findTrendingBattles(any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of(battle));
        when(battleOptionRepository.findByBattle(battle)).thenReturn(List.<BattleOption>of());
        when(battleTagRepository.findByBattle(battle)).thenReturn(List.<BattleTag>of());
        when(battleConverter.toSummaryResponse(eq(battle), eq(List.<Tag>of()), eq(List.<BattleOption>of())))
                .thenReturn(summary);

        var result = battleService.getHomeTrendingBattles();

        assertThat(result).containsExactly(summary);
    }

    @Test
    void getHomeNewBattles_제외아이디가_비어있으면_조회용_기본값을_사용한다() {
        BattleSummaryResponse summary = new BattleSummaryResponse(
                UUID.randomUUID(),
                "new",
                "summary",
                "thumbnail",
                BattleType.BATTLE,
                1,
                2L,
                3,
                List.of(),
                List.of()
        );

        when(battleRepository.findNewBattlesExcluding(any(), any(Pageable.class))).thenReturn(List.of(battle));
        when(battleOptionRepository.findByBattle(battle)).thenReturn(List.<BattleOption>of());
        when(battleTagRepository.findByBattle(battle)).thenReturn(List.<BattleTag>of());
        when(battleConverter.toSummaryResponse(eq(battle), eq(List.<Tag>of()), eq(List.<BattleOption>of())))
                .thenReturn(summary);

        var result = battleService.getHomeNewBattles(List.of());

        assertThat(result).containsExactly(summary);
    }
}
