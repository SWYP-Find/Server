package com.swyp.picke.domain.classroom.service.debate;

import com.swyp.picke.domain.classroom.service.report.ClassModerationClient;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 반박을 저장하기 전에 차단 대상인지 본다 (PRD §8 "차단 대상 입력은 저장 단계로 진행하지 않고 수정 안내", §11 "라운드를 소비하지 않는다").
 * <ul>
 *   <li>개인정보: 서버 규칙으로 먼저 본다 (AI 호출 없음)</li>
 *   <li>위협·혐오: Moderation 범주 중 {@link #BLOCKED_CATEGORIES} 만 막는다. 폭력·범죄·혐오를 다루는 정상 토론은 막지 않는다 (PRD §9)</li>
 *   <li>Moderation 장애면 토론을 멈추지 않고 통과시킨다. 반박은 AI 프롬프트에서 이미 비신뢰 데이터로 다룬다</li>
 * </ul>
 * 반박 원문은 로그에 남기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassDebateInputGuard {

    static final Set<String> BLOCKED_CATEGORIES = Set.of(
            "harassment/threatening",
            "hate/threatening",
            "sexual/minors",
            "self-harm/instructions",
            "illicit/violent"
    );

    private final ClassModerationClient moderationClient;

    /** 길이 규칙을 벗어난 반박은 저장 단계에서 400 이 나므로 Moderation 을 부르지 않는다 */
    public void check(String rawContent) {
        String content = rawContent == null ? "" : rawContent.strip();
        if (PersonalInfoDetector.contains(content)) {
            throw new CustomException(ErrorCode.CLASS_DEBATE_REPLY_PERSONAL_INFO);
        }
        int length = content.codePointCount(0, content.length());
        if (length < ClassDebateSessionService.REPLY_MIN_LENGTH || length > ClassDebateSessionService.REPLY_MAX_LENGTH) {
            return;
        }
        Set<String> flagged;
        try {
            flagged = moderationClient.flaggedCategories(content);
        } catch (Exception e) {
            log.warn("[ClassDebate] moderation unavailable, reply allowed : {}", e.toString());
            return;
        }
        Set<String> blocked = flagged.stream().filter(BLOCKED_CATEGORIES::contains).collect(Collectors.toSet());
        if (!blocked.isEmpty()) {
            log.info("[ClassDebate] reply blocked categories={}", blocked);
            throw new CustomException(ErrorCode.CLASS_DEBATE_REPLY_BLOCKED);
        }
    }
}
