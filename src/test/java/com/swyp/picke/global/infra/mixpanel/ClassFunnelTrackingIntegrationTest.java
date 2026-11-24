package com.swyp.picke.global.infra.mixpanel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 트래커 → 커밋 후 비동기 전송 연결 확인 (#689). 실제 Mixpanel 대신 전송기를 바꿔 끼운다 */
@SpringBootTest(properties = "picke.analytics.mixpanel.tracking-enabled=true")
@ActiveProfiles("test")
class ClassFunnelTrackingIntegrationTest {

    @Autowired
    private ClassFunnelTracker tracker;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private MixpanelImportSender sender;

    private User newUser() {
        return userRepository.save(User.builder()
                .userTag("trk-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname("트래킹")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("커밋되면 userTag 로 Mixpanel 에 보내고, 롤백되면 보내지 않는다")
    @SuppressWarnings("unchecked")
    void sendsAfterCommitOnly() {
        when(sender.send(anyList())).thenReturn(true);
        User user = newUser();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> tracker.track(ClassFunnelTracker.CLASS_CREATE_START, user.getId(), null,
                Map.of("battle_type", "CUSTOM")));

        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(sender, timeout(3000)).send(captor.capture());
        Map<String, Object> payload = captor.getValue().get(0);
        assertThat(payload).containsEntry("event", "class_create_start");
        assertThat((Map<String, Object>) payload.get("properties"))
                .containsEntry("distinct_id", user.getUserTag())
                .containsEntry("battle_type", "CUSTOM");

        org.mockito.Mockito.clearInvocations(sender);
        tx.executeWithoutResult(status -> {
            tracker.track(ClassFunnelTracker.CLASS_VOTE_PRE, user.getId(), null);
            status.setRollbackOnly();
        });
        verify(sender, after(1000).never()).send(anyList());
        userRepository.deleteById(user.getId());
    }
}
