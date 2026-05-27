package com.swyp.picke.domain.perspective.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleOptionLabel;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.perspective.dto.response.CommentListResponse;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.perspective.repository.CommentLikeRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerspectiveCommentServiceTest {

    @Mock
    private PerspectiveRepository perspectiveRepository;

    @Mock
    private PerspectiveCommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private UserService userQueryService;

    @Mock
    private BattleVoteService battleVoteService;

    @Mock
    private BattleService battleService;

    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private PerspectiveCommentService commentService;

    @Test
    @DisplayName("댓글 목록 응답의 stance는 A/B 대신 투표한 의견 문구를 반환한다")
    void getComments_returns_option_opinion_as_stance() {
        User writer = user(1L);
        User commenter = user(2L);
        Battle battle = battle(100L);
        BattleOption perspectiveOption = option(10L, battle, BattleOptionLabel.B, "예술이다");
        BattleOption votedOption = option(11L, battle, BattleOptionLabel.A, "예술이 아니다");
        Perspective perspective = Perspective.builder()
                .battle(battle)
                .user(writer)
                .option(perspectiveOption)
                .content("관점 내용")
                .build();
        ReflectionTestUtils.setField(perspective, "id", 1000L);
        PerspectiveComment comment = PerspectiveComment.builder()
                .perspective(perspective)
                .user(commenter)
                .content("댓글 내용")
                .build();
        ReflectionTestUtils.setField(comment, "id", 2000L);

        when(perspectiveRepository.findById(1000L)).thenReturn(Optional.of(perspective));
        when(commentRepository.findByPerspectiveOrderByCreatedAtDesc(eq(perspective), any(Pageable.class)))
                .thenReturn(List.of(comment));
        when(userQueryService.findSummaryById(2L)).thenReturn(new UserSummary("user-2", "nick", "OWL"));
        when(s3PresignedUrlService.generatePresignedUrl(anyString())).thenReturn("character-url");
        when(battleVoteService.findPostVoteOptionId(100L, 2L)).thenReturn(11L);
        when(battleService.findOptionById(11L)).thenReturn(votedOption);

        CommentListResponse response = commentService.getComments(1000L, 1L, null, null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().stance()).isEqualTo("예술이 아니다");
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
