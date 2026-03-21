package com.swyp.app.domain.battle.service;

import com.swyp.app.domain.battle.converter.BattleConverter;
import com.swyp.app.domain.battle.dto.response.TodayBattleResponse;
import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.entity.BattleTag;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import com.swyp.app.domain.battle.enums.BattleStatus;
import com.swyp.app.domain.battle.enums.BattleType;
import com.swyp.app.domain.battle.repository.BattleOptionRepository;
import com.swyp.app.domain.battle.repository.BattleRepository;
import com.swyp.app.domain.battle.repository.BattleTagRepository;
import com.swyp.app.domain.tag.entity.Tag;
import com.swyp.app.domain.tag.enums.TagType;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleServiceImplV2Test {

    @Mock
    private BattleRepository battleRepository;
    @Mock
    private BattleOptionRepository battleOptionRepository;
    @Mock
    private BattleTagRepository battleTagRepository;
    @Mock
    private BattleConverter battleConverter;

    @InjectMocks
    private BattleServiceImplV2 battleService;

    private Battle battle;
    private BattleOption optionA;
    private BattleOption optionB;
    private Tag tag;

    @BeforeEach
    void setUp() {
        battle = Battle.builder()
                .title("battle")
                .summary("summary")
                .thumbnailUrl("thumbnail")
                .type(BattleType.BATTLE)
                .targetDate(LocalDate.now())
                .status(BattleStatus.PUBLISHED)
                .build();

        optionA = BattleOption.builder()
                .battle(battle)
                .label(BattleOptionLabel.A)
                .title("A")
                .stance("stance-a")
                .representative("rep-a")
                .imageUrl("image-a")
                .build();

        optionB = BattleOption.builder()
                .battle(battle)
                .label(BattleOptionLabel.B)
                .title("B")
                .stance("stance-b")
                .representative("rep-b")
                .imageUrl("image-b")
                .build();

        tag = Tag.builder()
                .name("태그")
                .type(TagType.CATEGORY)
                .build();
    }

    @Test
    void getTrendingBattles_배치조회한_태그와_옵션으로_변환한다() {
        BattleTag battleTag = BattleTag.builder().battle(battle).tag(tag).build();
        TodayBattleResponse response = new TodayBattleResponse(
                UUID.randomUUID(),
                "title",
                "summary",
                "thumbnail",
                BattleType.BATTLE,
                1,
                2L,
                3,
                List.of(),
                List.of()
        );

        when(battleRepository.findTrendingBattles(any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of(battle));
        when(battleTagRepository.findByBattleIn(List.of(battle))).thenReturn(List.of(battleTag));
        when(battleOptionRepository.findByBattleInOrderByBattleIdAscLabelAsc(List.of(battle))).thenReturn(List.of(optionA, optionB));
        when(battleConverter.toTodayResponse(eq(battle), eq(List.of(tag)), eq(List.of(optionA, optionB))))
                .thenReturn(response);

        var result = battleService.getTrendingBattles();

        assertThat(result).containsExactly(response);
        verify(battleTagRepository).findByBattleIn(List.of(battle));
        verify(battleOptionRepository).findByBattleInOrderByBattleIdAscLabelAsc(List.of(battle));
    }
}
