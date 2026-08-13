package com.swyp.picke.domain.battle.service;

import com.swyp.picke.domain.battle.entity.BattleOptionTag;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.battle.repository.BattleTagRepository;
import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.enums.TagType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleQueryServiceTest {

    @Mock
    private BattleRepository battleRepository;
    @Mock
    private BattleOptionRepository battleOptionRepository;
    @Mock
    private BattleTagRepository battleTagRepository;
    @Mock
    private BattleOptionTagRepository battleOptionTagRepository;

    @InjectMocks
    private BattleQueryService battleQueryService;

    @Test
    @DisplayName("철학자 유형 집계에서 제거된 철학자 태그를 처리하지 않는다")
    void getTopPhilosopher_ignoresUnsupportedTags() {
        BattleOptionTag supported = optionTag("플라톤");
        BattleOptionTag removed = optionTag("데카르트");
        when(battleOptionTagRepository.findByBattleOptionIdIn(List.of(1L)))
                .thenReturn(List.of(removed, removed, supported));

        assertThat(battleQueryService.getTopPhilosopherTagNameFromOptions(List.of(1L)))
                .contains("플라톤");
    }

    private BattleOptionTag optionTag(String name) {
        return BattleOptionTag.builder()
                .tag(Tag.builder().name(name).type(TagType.PHILOSOPHER).build())
                .build();
    }
}
