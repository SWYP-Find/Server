package com.swyp.picke.global.infra.mixpanel;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 클래스 퍼널 이벤트를 남긴다 (#689). 호출한 곳의 트랜잭션이 커밋돼야 보내지고(롤백되면 안 보냄),
 * 전송은 비동기라 API 응답 시간·성공 여부에 영향을 주지 않는다.
 *
 * <p>dev·prod 서버가 같은 Mixpanel 프로젝트를 쓰므로 기본은 꺼져 있고, 운영 env {@code MIXPANEL_TRACKING_ENABLED=true} 로만 켠다.
 */
@Component
public class ClassFunnelTracker {

    public static final String CLASS_CREATE_START = "class_create_start";
    public static final String CLASS_CREATE_COMPLETE = "class_create_complete";
    public static final String CLASS_JOIN_PREVIEW = "class_join_preview";
    public static final String CLASS_JOIN_COMPLETE = "class_join_complete";
    public static final String CLASS_VOTE_PRE = "class_vote_pre";
    public static final String CLASS_TTS_COMPLETE = "class_tts_complete";
    public static final String CLASS_VOTE_MID = "class_vote_mid";
    public static final String CLASS_VOTE_POST = "class_vote_post";
    public static final String CLASS_OPINION_WRITE = "class_opinion_write";
    public static final String CLASS_COMMENT_WRITE = "class_comment_write";
    public static final String CLASS_DEBATE_START = "class_debate_start";
    public static final String CLASS_DEBATE_REPLY = "class_debate_reply";
    public static final String CLASS_RESULT_VIEW = "class_result_view";
    public static final String CLASS_REPORT_VIEW = "class_report_view";

    private final ApplicationEventPublisher publisher;
    private final boolean enabled;

    public ClassFunnelTracker(ApplicationEventPublisher publisher,
                              @Value("${picke.analytics.mixpanel.tracking-enabled:${MIXPANEL_TRACKING_ENABLED:false}}") boolean enabled) {
        this.publisher = publisher;
        this.enabled = enabled;
    }

    public void track(String event, Long userId, Long classId) {
        track(event, userId, classId, Map.of());
    }

    public void track(String event, Long userId, Long classId, Map<String, Object> properties) {
        if (!enabled || userId == null) {
            return;
        }
        publisher.publishEvent(new ClassFunnelEvent(event, userId, classId,
                properties == null ? Map.of() : Map.copyOf(properties), Instant.now()));
    }
}
