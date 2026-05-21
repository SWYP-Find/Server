package com.swyp.picke.domain.perspective.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.perspective.dto.response.PerspectiveDetailResponse;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveLikeRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.user.dto.response.UserSummary;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.domain.vote.service.BattleVoteService;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerspectiveServiceTest {

    @Mock
    private PerspectiveRepository perspectiveRepository;

    @Mock
    private PerspectiveCommentRepository perspectiveCommentRepository;

    @Mock
    private PerspectiveLikeRepository perspectiveLikeRepository;

    @Mock
    private BattleService battleService;

    @Mock
    private BattleVoteService battleVoteService;

    @Mock
    private UserService userQueryService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GptModerationService gptModerationService;

    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private PerspectiveService perspectiveService;

    @Test
    @DisplayName("관점 상세 응답의 옵션 label은 A/B 대신 의견 문구를 반환한다")
    void getPerspectiveDetail_returns_option_opinion_as_label() {
        User user = user(1L);
        BattleOption option = option(10L, battle(100L), BattleOptionLabel.A, "예술이 아니다");
        Perspective perspective = Perspective.builder()
                .battle(option.getBattle())
                .user(user)
                .option(option)
                .content("관점 내용")
                .build();
        perspective.publish();
        ReflectionTestUtils.setField(perspective, "id", 1000L);

        when(perspectiveRepository.findById(1000L)).thenReturn(Optional.of(perspective));
        when(userQueryService.findSummaryById(1L)).thenReturn(new UserSummary("user-1", "nick", "OWL"));
        when(s3PresignedUrlService.generatePresignedUrl(anyString())).thenReturn("character-url");
        when(perspectiveLikeRepository.existsByPerspectiveAndUserId(perspective, 1L)).thenReturn(false);

        PerspectiveDetailResponse response = perspectiveService.getPerspectiveDetail(1000L, 1L);

        assertThat(response.option().label()).isEqualTo("예술이 아니다");
        assertThat(response.option().title()).isEqualTo("예술이 아니다");
    }

    private Battle battle(Long id) {
        Battle battle = Battle.builder()
                .title("battle")
                .summary("summary")
                .targetDate(LocalDate.now())
                .status(BattleStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(battle, "id", id);
        return battle;
    }

    private BattleOption option(Long id, Battle battle, BattleOptionLabel label, String title) {
        BattleOption option = BattleOption.builder()
                .battle(battle)
                .title(title)
                .stance("stance")
                .displayOrder(label == BattleOptionLabel.A ? 1 : 2)
                .build();
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private User user(Long id) {
        User user = User.builder()
                .userTag("user-" + id)
                .nickname("nick")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
