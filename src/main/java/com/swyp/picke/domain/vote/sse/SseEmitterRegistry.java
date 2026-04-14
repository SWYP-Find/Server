package com.swyp.picke.domain.vote.sse;

import com.swyp.picke.domain.vote.dto.response.VoteStatsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterRegistry {

    private static final long TIMEOUT_MS = 180_000L; // 3분

    // battleId -> (userId -> SseEmitter)
    private final Map<Long, Map<Long, SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long battleId, Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        // 기존 연결이 있으면 먼저 제거
        remove(battleId, userId);

        emitters.computeIfAbsent(battleId, k -> new ConcurrentHashMap<>())
                .put(userId, emitter);

        emitter.onCompletion(() -> remove(battleId, userId));
        emitter.onTimeout(() -> remove(battleId, userId));
        emitter.onError(e -> remove(battleId, userId));

        return emitter;
    }

    public void remove(Long battleId, Long userId) {
        Map<Long, SseEmitter> battleEmitters = emitters.get(battleId);
        if (battleEmitters == null) return;

        battleEmitters.remove(userId);
        if (battleEmitters.isEmpty()) {
            emitters.remove(battleId);
        }
    }

    public void sendToAll(Long battleId, VoteStatsResponse stats) {
        Map<Long, SseEmitter> battleEmitters = emitters.get(battleId);
        if (battleEmitters == null || battleEmitters.isEmpty()) return;

        battleEmitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("vote-stats")
                        .data(stats));
            } catch (IOException e) {
                log.warn("SSE 전송 실패 - battleId: {}, userId: {}", battleId, userId);
                remove(battleId, userId);
            }
        });
    }

    // nginx/프록시 idle 끊김 방지용 heartbeat (30초마다)
    @Scheduled(fixedDelay = 30_000)
    public void sendHeartbeat() {
        emitters.forEach((battleId, battleEmitters) ->
                battleEmitters.forEach((userId, emitter) -> {
                    try {
                        emitter.send(SseEmitter.event().comment("ping"));
                    } catch (IOException e) {
                        remove(battleId, userId);
                    }
                })
        );
    }
}
