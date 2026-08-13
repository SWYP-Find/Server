package com.swyp.picke.domain.tag.service;

import com.swyp.picke.domain.admin.dto.tag.request.TagRequest;
import com.swyp.picke.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.battle.repository.BattleTagRepository;
import com.swyp.picke.domain.tag.dto.response.TagListResponse;
import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.tag.repository.TagRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagRepository tagRepository;
    @Mock
    private BattleTagRepository battleTagRepository;
    @Mock
    private BattleOptionTagRepository battleOptionTagRepository;
    @Mock
    private BattleRepository battleRepository;

    @InjectMocks
    private TagServiceImpl tagService;

    @Test
    @DisplayName("철학자 태그 목록에서 지원하지 않는 철학자를 제외한다")
    void getTags_filtersUnsupportedPhilosophers() {
        Tag supported = Tag.builder().name("소크라테스").type(TagType.PHILOSOPHER).build();
        Tag removed = Tag.builder().name("데카르트").type(TagType.PHILOSOPHER).build();
        when(tagRepository.findAllByTypeAndDeletedAtIsNull(TagType.PHILOSOPHER))
                .thenReturn(List.of(supported, removed));

        TagListResponse response = tagService.getTags(TagType.PHILOSOPHER);

        assertThat(response.items()).extracting("name").containsExactly("소크라테스");
        assertThat(response.totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("지원하지 않는 철학자 태그는 생성할 수 없다")
    void createTag_rejectsUnsupportedPhilosopher() {
        TagRequest request = new TagRequest("데카르트", TagType.PHILOSOPHER);

        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
