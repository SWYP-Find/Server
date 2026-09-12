package com.swyp.picke.domain.battle.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleOptionRequest;
import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleUpdateRequest;
import com.swyp.picke.domain.battle.converter.BattleConverter;
import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.entity.BattleOptionTag;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleOptionTagRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.battle.repository.BattleTagRepository;
import com.swyp.picke.domain.notification.service.NotificationDispatchService;
import com.swyp.picke.domain.scenario.repository.ScenarioRepository;
import com.swyp.picke.domain.scenario.service.ScenarioAudioPipelineService;
import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.enums.TagType;
import com.swyp.picke.domain.tag.repository.TagRepository;
import com.swyp.picke.domain.user.service.UserBattleService;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import com.swyp.picke.global.infra.local.service.LocalDraftFileStorageService;
import com.swyp.picke.global.infra.s3.service.S3UploadService;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BattleServiceImplTest {

    @Mock private BattleRepository battleRepository;
    @Mock private BattleOptionRepository battleOptionRepository;
    @Mock private BattleTagRepository battleTagRepository;
    @Mock private BattleOptionTagRepository battleOptionTagRepository;
    @Mock private TagRepository tagRepository;
    @Mock private UserRepository userRepository;
    @Mock private BattleVoteRepository battleVoteRepository;
    @Mock private BattleConverter battleConverter;
    @Mock private S3UploadService s3UploadService;
    @Mock private LocalDraftFileStorageService localDraftFileStorageService;
    @Mock private UserBattleService userBattleService;
    @Mock private BattleAutoSeedService battleAutoSeedService;
    @Mock private ScenarioRepository scenarioRepository;
    @Mock private ScenarioAudioPipelineService scenarioAudioPipelineService;
    @Mock private NotificationDispatchService notificationDispatchService;

    private BattleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BattleServiceImpl(
                battleRepository, battleOptionRepository, battleTagRepository, battleOptionTagRepository,
                tagRepository, userRepository, battleVoteRepository, battleConverter, s3UploadService,
                localDraftFileStorageService, userBattleService, battleAutoSeedService, scenarioRepository,
                scenarioAudioPipelineService, notificationDispatchService);
    }

    private Battle battle(Long id) {
        Battle battle = Battle.builder()
                .title("제목").status(BattleStatus.PENDING)
                .creatorType(BattleCreatorType.ADMIN)
                .build();
        ReflectionTestUtils.setField(battle, "id", id);
        return battle;
    }

    private BattleOption option(Long id, Battle battle) {
        BattleOption option = BattleOption.builder()
                .battle(battle).title("기존 옵션").displayOrder(1).build();
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    @Test
    void 옵션_태그를_교체할때_기존_태그_삭제를_먼저_flush한_뒤_새_태그를_저장한다() {
        Battle battle = battle(22L);
        BattleOption existingOption = option(42L, battle);
        Tag tag = Tag.builder().name("칸트").type(TagType.PHILOSOPHER).build();
        ReflectionTestUtils.setField(tag, "id", 6L);
        BattleOptionTag existingOptionTag = BattleOptionTag.builder()
                .battleOption(existingOption).tag(tag).build();

        when(battleRepository.findById(22L)).thenReturn(Optional.of(battle));
        when(battleOptionRepository.findByBattle(battle)).thenReturn(List.of(existingOption));
        when(battleOptionTagRepository.findByBattleOption(existingOption)).thenReturn(List.of(existingOptionTag));
        when(tagRepository.findAllById(anyList())).thenReturn(List.of(tag));
        lenient().when(battleOptionTagRepository.findByBattleWithTags(battle)).thenReturn(List.of());
        lenient().when(battleOptionRepository.save(any(BattleOption.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AdminBattleOptionRequest optionRequest = new AdminBattleOptionRequest(
                "기존 옵션", null, null, null, 1, List.of(6L));
        AdminBattleUpdateRequest request = new AdminBattleUpdateRequest(
                "제목", null, null, null, null, null, null, BattleStatus.PENDING,
                null, List.of(optionRequest, optionRequest2()));

        service.updateBattle(22L, request);

        // 같은 (battleOption, tag) 조합을 다시 저장하는 경우, delete가 flush로 먼저 반영된 뒤에
        // insert(save)가 나가야 유니크 제약 위반(23505)이 안 난다.
        InOrder inOrder = inOrder(battleOptionTagRepository);
        inOrder.verify(battleOptionTagRepository).deleteAll(List.of(existingOptionTag));
        inOrder.verify(battleOptionTagRepository).flush();
        inOrder.verify(battleOptionTagRepository).save(any(BattleOptionTag.class));
    }

    // updateBattle은 옵션 2~4개를 요구한다(validateBattleOptionCount) — 최소 개수 충족용 더미 옵션
    private AdminBattleOptionRequest optionRequest2() {
        return new AdminBattleOptionRequest("두번째 옵션", null, null, null, 2, List.of());
    }
}
