package com.swyp.picke.domain.classroom.service.debate;

import com.swyp.picke.domain.classroom.service.report.ClassModerationClient;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 반박을 저장하기 전에 차단·반려 대상인지 본다 (PRD §8 "차단 대상 입력은 저장 단계로 진행하지 않고 수정 안내", §11 "라운드를 소비하지 않는다",
 * 엣지 케이스 PRD 2.3 "무의미한 입력과 프롬프트 인젝션은 입력 단계에서 반려").
 * <ul>
 *   <li>개인정보·무의미 입력: 서버 규칙으로 먼저 본다 (AI 호출 없음)</li>
 *   <li>위협·혐오: Moderation 범주 중 {@link #BLOCKED_CATEGORIES} 만 막는다. 폭력·범죄·혐오를 다루는 정상 토론은 막지 않는다 (PRD §9)</li>
 *   <li>인젝션·무의미: 입력 분류 AI 로 본다. Moderation 과 동시에 호출해 기다리는 시간을 줄인다</li>
 *   <li>Moderation·분류 장애면 토론을 멈추지 않고 통과시킨다. 반박은 AI 프롬프트에서 이미 비신뢰 데이터로 다룬다</li>
 * </ul>
 * 반박 원문은 로그에 남기지 않는다. 반려된 반박은 저장하지 않는다.
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
    /** 외부 호출을 기다리는 최대 시간. 넘으면 그 검사는 통과로 본다 */
    static final long CHECK_TIMEOUT_SECONDS = 10;

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final ClassModerationClient moderationClient;
    private final ClassDebateInputClassifier classifier;

    /**
     * 차단·반려 대상이면 CustomException. 접수할 반박이면 입력 분류 결과를 돌려준다.
     * 분류하지 못했거나(장애·맥락 없음) 길이 규칙을 벗어나 저장 단계에서 400 이 날 반박이면 null.
     *
     * @param context 분류 맥락. 반박을 받을 수 없는 상태라 저장 단계에서 거절될 요청이면 null 이고 분류하지 않는다
     */
    public ClassDebateInputClassifier.Result check(String rawContent, ClassDebateInputClassifier.Context context) {
        String content = rawContent == null ? "" : rawContent.strip();
        if (PersonalInfoDetector.contains(content)) {
            throw new CustomException(ErrorCode.CLASS_DEBATE_REPLY_PERSONAL_INFO);
        }
        int length = content.codePointCount(0, content.length());
        if (length < ClassDebateSessionService.REPLY_MIN_LENGTH || length > ClassDebateSessionService.REPLY_MAX_LENGTH) {
            return null;
        }
        if (MeaninglessInputDetector.isMeaningless(content)) {
            log.info("[ClassDebate] reply rejected reason=MEANINGLESS source=rule");
            throw new CustomException(ErrorCode.CLASS_DEBATE_REPLY_MEANINGLESS);
        }

        CompletableFuture<Set<String>> moderation = CompletableFuture.supplyAsync(() -> moderationClient.flaggedCategories(content), EXECUTOR);
        CompletableFuture<ClassDebateInputClassifier.Result> classification = context == null
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.supplyAsync(() -> classifier.classify(content, context), EXECUTOR);

        Set<String> flagged = await(moderation, "moderation");
        if (flagged != null) {
            Set<String> blocked = flagged.stream().filter(BLOCKED_CATEGORIES::contains).collect(Collectors.toSet());
            if (!blocked.isEmpty()) {
                classification.cancel(true);
                log.info("[ClassDebate] reply blocked categories={}", blocked);
                throw new CustomException(ErrorCode.CLASS_DEBATE_REPLY_BLOCKED);
            }
        }
        ClassDebateInputClassifier.Result result = await(classification, "classification");
        if (result == null) {
            return null;
        }
        if (result.isInjection()) {
            log.info("[ClassDebate] reply rejected reason=INJECTION types={} prompt={}", result.typesValue(), ClassDebateInputClassifier.PROMPT_VERSION);
            throw new CustomException(ErrorCode.CLASS_DEBATE_REPLY_INJECTION);
        }
        if (result.isMeaningless()) {
            log.info("[ClassDebate] reply rejected reason=MEANINGLESS source=ai types={} prompt={}", result.typesValue(), ClassDebateInputClassifier.PROMPT_VERSION);
            throw new CustomException(ErrorCode.CLASS_DEBATE_REPLY_MEANINGLESS);
        }
        return result;
    }

    private static <T> T await(CompletableFuture<T> future, String name) {
        try {
            return future.get(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            future.cancel(true);
            log.warn("[ClassDebate] {} unavailable, reply allowed : {}", name, e.toString());
            return null;
        }
    }
}
