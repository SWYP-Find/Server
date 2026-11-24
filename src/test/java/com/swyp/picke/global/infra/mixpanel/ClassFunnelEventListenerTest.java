package com.swyp.picke.global.infra.mixpanel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

/** 클래스 퍼널 이벤트 → Mixpanel 이벤트 변환 (#689) */
class ClassFunnelEventListenerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ClassroomRepository classroomRepository = mock(ClassroomRepository.class);
    private final ClassFunnelEventListener listener = new ClassFunnelEventListener(userRepository, classroomRepository,
            events -> true, mock(PlatformTransactionManager.class));

    private Classroom classroom(ClassBattleType type, long ownerId) {
        Classroom classroom = mock(Classroom.class);
        User owner = mock(User.class);
        Battle battle = mock(Battle.class);
        when(owner.getId()).thenReturn(ownerId);
        when(battle.getId()).thenReturn(171L);
        when(classroom.getId()).thenReturn(31L);
        when(classroom.getOwner()).thenReturn(owner);
        when(classroom.getBattle()).thenReturn(battle);
        when(classroom.getBattleType()).thenReturn(type);
        when(classroom.getCapacity()).thenReturn(15);
        when(classroom.isOwnerParticipates()).thenReturn(true);
        return classroom;
    }

    private void user(long id, String tag) {
        User user = mock(User.class);
        when(user.getUserTag()).thenReturn(tag);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("distinct_id 는 userTag, 클래스 속성(class_id·battle_type·role·capacity·battle_id)과 $insert_id 를 붙인다")
    void buildsMixpanelEvent() {
        user(120L, "pique-abc");
        Classroom classroom = classroom(ClassBattleType.TTS, 120L);
        when(classroomRepository.findById(31L)).thenReturn(Optional.of(classroom));
        Instant at = Instant.parse("2026-11-24T03:00:00Z");

        Map<String, Object> payload = listener.toMixpanelEvent(new ClassFunnelEvent(
                ClassFunnelTracker.CLASS_REPORT_VIEW, 120L, 31L, Map.of("report_type", "INSIGHT"), at));

        assertThat(payload).containsEntry("event", "class_report_view");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) payload.get("properties");
        assertThat(properties)
                .containsEntry("distinct_id", "pique-abc")
                .containsEntry("time", at.toEpochMilli())
                .containsEntry("class_id", 31L)
                .containsEntry("battle_id", 171L)
                .containsEntry("battle_type", "EXISTING")
                .containsEntry("role", "OWNER")
                .containsEntry("capacity", 15)
                .containsEntry("owner_participates", true)
                .containsEntry("source", "server")
                .containsEntry("report_type", "INSIGHT")
                .containsKey("$insert_id");
    }

    @Test
    @DisplayName("운영자가 아니면 MEMBER, 커스텀은 CUSTOM. 이벤트별 속성이 공통 속성보다 우선한다")
    void memberAndOverride() {
        user(7L, "pique-member");
        Classroom classroom = classroom(ClassBattleType.CUSTOM, 120L);
        when(classroomRepository.findById(31L)).thenReturn(Optional.of(classroom));

        Map<String, Object> payload = listener.toMixpanelEvent(new ClassFunnelEvent(
                ClassFunnelTracker.CLASS_DEBATE_REPLY, 7L, 31L, Map.of("round", 2, "role", "MEMBER"), Instant.now()));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) payload.get("properties");
        assertThat(properties).containsEntry("role", "MEMBER").containsEntry("battle_type", "CUSTOM").containsEntry("round", 2);
    }

    @Test
    @DisplayName("개설 시작처럼 클래스가 없으면 클래스 속성 없이, 사용자가 없으면 보내지 않는다")
    void withoutClassOrUser() {
        user(120L, "pique-abc");
        Map<String, Object> payload = listener.toMixpanelEvent(new ClassFunnelEvent(
                ClassFunnelTracker.CLASS_CREATE_START, 120L, null, Map.of("battle_type", "CUSTOM"), Instant.now()));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) payload.get("properties");
        assertThat(properties).containsEntry("battle_type", "CUSTOM").doesNotContainKey("class_id");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThat(listener.toMixpanelEvent(new ClassFunnelEvent("class_vote_pre", 999L, 31L, Map.of(), Instant.now()))).isNull();
    }
}
