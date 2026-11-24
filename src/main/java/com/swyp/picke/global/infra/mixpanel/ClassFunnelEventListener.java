package com.swyp.picke.global.infra.mixpanel;

import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 커밋된 클래스 퍼널 이벤트를 Mixpanel 이벤트로 바꿔 보낸다 (#689).
 *
 * <ul>
 *   <li>distinct_id = userTag (앱이 로그인 때 identify 하는 값) → 앱 이벤트와 같은 사용자로 합쳐진다</li>
 *   <li>공통 속성: class_id, battle_id, battle_type(EXISTING/CUSTOM), role(OWNER/MEMBER), capacity, owner_participates, source=server</li>
 *   <li>$insert_id 로 같은 이벤트가 두 번 들어가도 Mixpanel 이 하나로 합친다</li>
 *   <li>실패해도 다시 시도하지 않는다. 퍼널 지표용이라 몇 건 빠져도 업무에 영향이 없다</li>
 * </ul>
 */
@Slf4j
@Component
public class ClassFunnelEventListener {

    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final MixpanelImportSender sender;
    private final TransactionTemplate readOnly;

    public ClassFunnelEventListener(UserRepository userRepository, ClassroomRepository classroomRepository,
                                    MixpanelImportSender sender, PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.classroomRepository = classroomRepository;
        this.sender = sender;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(ClassFunnelEvent event) {
        try {
            Map<String, Object> payload = readOnly.execute(status -> toMixpanelEvent(event));
            if (payload != null) {
                sender.send(List.of(payload));
            }
        } catch (Exception e) {
            log.warn("[Mixpanel] 클래스 퍼널 이벤트 변환 실패 event={} : {}", event.event(), e.getClass().getSimpleName());
        }
    }

    Map<String, Object> toMixpanelEvent(ClassFunnelEvent event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null || user.getUserTag() == null) {
            return null;
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("time", event.occurredAt().toEpochMilli());
        properties.put("distinct_id", user.getUserTag());
        properties.put("$insert_id", UUID.randomUUID().toString().replace("-", ""));
        properties.put("source", "server");
        if (event.classId() != null) {
            Classroom classroom = classroomRepository.findById(event.classId()).orElse(null);
            if (classroom != null) {
                properties.put("class_id", classroom.getId());
                properties.put("battle_type", battleType(classroom.getBattleType()));
                properties.put("role", classroom.getOwner() != null && event.userId().equals(classroom.getOwner().getId())
                        ? "OWNER" : "MEMBER");
                properties.put("capacity", classroom.getCapacity());
                properties.put("owner_participates", classroom.isOwnerParticipates());
                if (classroom.getBattle() != null) {
                    properties.put("battle_id", classroom.getBattle().getId());
                }
            }
        }
        // 이벤트별 속성이 공통 속성보다 우선한다 (예: 개설 시작의 battle_type)
        properties.putAll(event.properties());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", event.event());
        payload.put("properties", properties);
        return payload;
    }

    /** 앱·기획 용어에 맞춰 TTS(기존 콘텐츠) → EXISTING 으로 보낸다 */
    static String battleType(ClassBattleType type) {
        if (type == null) {
            return null;
        }
        return type == ClassBattleType.CUSTOM ? "CUSTOM" : "EXISTING";
    }
}
