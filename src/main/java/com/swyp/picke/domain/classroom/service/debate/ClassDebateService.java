package com.swyp.picke.domain.classroom.service.debate;

import com.swyp.picke.domain.classroom.dto.request.ClassDebateReplyRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassDebateResponse;
import com.swyp.picke.domain.classroom.service.report.AiProviderErrors;
import com.swyp.picke.domain.classroom.service.report.ClassReportAiClient;
import com.swyp.picke.domain.classroom.service.report.ReportValidationException;
import com.swyp.picke.domain.classroom.service.topic.AiUsage;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * AI 철학자 토론 오케스트레이터 (PRD §8, §11). 트랜잭션을 열지 않고 (1) 준비 → (2) AI 호출 → (3) 반영 을 잇는다.
 * <ul>
 *   <li>라운드 1건당 제공자 호출은 최초 포함 최대 3회. 연결 오류·429·5xx 는 지연 후 재시도, 규칙 위반은 1회 보정</li>
 *   <li>실패하면 세션을 FAILED 로 두고 재시도 API 로 같은 작업을 다시 실행한다. 검증 전 텍스트는 대화로 저장하지 않는다</li>
 *   <li>요청 스레드에서 끝까지 처리한다. 중간에 나가도 서버는 결과를 저장하므로 재접속하면 같은 대화를 받는다</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassDebateService {

    static final int MAX_PROVIDER_CALLS = 3;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ClassDebateSessionService sessionService;
    private final ClassReportAiClient aiClient;
    private final ClassDebateContentValidator validator;
    private final ClassDebateInputGuard inputGuard;

    /** 재시도 지연의 기준값. 1회차 뒤 base, 2회차 뒤 base×2 에 jitter 를 더한다 */
    long retryBackoffMillis = 500;

    public ClassDebateResponse get(Long userId, Long classId) {
        return sessionService.view(userId, classId, now());
    }

    public ClassDebateResponse start(Long userId, Long classId) {
        ClassDebateSessionService.PreparedTurn turn;
        try {
            turn = sessionService.start(userId, classId, aiClient.getModel(), now());
        } catch (DataIntegrityViolationException e) {
            // 두 기기에서 동시에 시작: 먼저 만든 세션을 그대로 돌려준다
            turn = null;
        }
        if (turn != null) {
            generate(turn);
        }
        return sessionService.view(userId, classId, now());
    }

    public ClassDebateResponse reply(Long userId, Long classId, ClassDebateReplyRequest request) {
        // 외부 검사는 세션 잠금 밖에서 한다. 차단되면 저장·라운드 소비 없이 끝난다
        inputGuard.check(request.content());
        ClassDebateSessionService.PreparedTurn turn = sessionService.reply(userId, classId, request, aiClient.getModel(), now());
        if (turn != null) {
            generate(turn);
        }
        return sessionService.view(userId, classId, now());
    }

    public ClassDebateResponse retry(Long userId, Long classId) {
        generate(sessionService.retry(userId, classId, aiClient.getModel(), now()));
        return sessionService.view(userId, classId, now());
    }

    private void generate(ClassDebateSessionService.PreparedTurn turn) {
        String userPrompt = ClassDebatePrompts.user(turn.input());
        String prompt = userPrompt;
        boolean corrected = false;
        boolean rejected = false;
        String lastError = null;
        AiUsage usage = new AiUsage();

        while (usage.calls() < MAX_PROVIDER_CALLS) {
            ClassReportAiClient.AiChatResult result;
            try {
                result = aiClient.chatJson(ClassDebatePrompts.SYSTEM, prompt, ClassDebatePrompts.MAX_OUTPUT_TOKENS);
                usage.add(result);
            } catch (ReportValidationException e) {
                // 잘린 출력·JSON 아님 (A13): 형식 위반으로 보고 보정 1회
                usage.addCall();
                lastError = e.getCode();
                rejected = true;
                if (corrected) {
                    break;
                }
                corrected = true;
                prompt = ClassDebatePrompts.correctionUser(userPrompt, "(완전한 JSON 이 아니었음)", "완전한 JSON 객체 하나만 출력하세요");
                continue;
            } catch (Exception e) {
                usage.addCall();
                lastError = AiProviderErrors.errorCodeOf(e);
                rejected = false;
                log.warn("[ClassDebate] provider call failed sessionId={} round={} : {}", turn.sessionId(), turn.round(), e.toString());
                if (!AiProviderErrors.retryable(e) || usage.calls() >= MAX_PROVIDER_CALLS) {
                    break;
                }
                backoff(usage.calls());
                continue;
            }
            try {
                sessionService.completeTurn(turn, validator.parse(result.json()), usage, now());
                return;
            } catch (DebateValidationException e) {
                log.info("[ClassDebate] argument rejected sessionId={} round={} : {}", turn.sessionId(), turn.round(), e.getProblem());
                lastError = e.getCode();
                rejected = true;
                if (corrected) {
                    break;
                }
                corrected = true;
                prompt = ClassDebatePrompts.correctionUser(userPrompt, result.rawContent(), e.getProblem());
            }
        }
        sessionService.failTurn(turn, lastError, rejected, usage, now());
    }

    private void backoff(int calls) {
        if (retryBackoffMillis <= 0) {
            return;
        }
        long delay = retryBackoffMillis * (1L << (calls - 1)) + ThreadLocalRandom.current().nextLong(retryBackoffMillis / 2 + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(KST);
    }
}
